package main.java.core.algorithm;

import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.FoodStorage;
import main.java.core.models.Location;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A clustering-based path planner optimized for large zoos (Level 4).
 * This planner groups enclosures by proximity and food type, creates efficient
 * sub-paths for each cluster, and then merges them into a complete path.
 */
public class ClusterPathPlanner {
    
    private final MockDistanceCalculator distanceCalculator;
    private final int maxClustersPerDiet; // Maximum number of clusters to create per diet type
    private final double clusterRadiusThreshold; // Distance threshold for considering enclosures in the same cluster
    
    /**
     * Creates a ClusterPathPlanner with a specific distance calculator and clustering parameters.
     * 
     * @param distanceCalculator the calculator to use for distance measurements
     * @param maxClustersPerDiet maximum number of clusters to create per diet type
     * @param clusterRadiusThreshold distance threshold for clustering
     */
    public ClusterPathPlanner(
            MockDistanceCalculator distanceCalculator,
            int maxClustersPerDiet,
            double clusterRadiusThreshold) {
        this.distanceCalculator = distanceCalculator;
        this.maxClustersPerDiet = maxClustersPerDiet;
        this.clusterRadiusThreshold = clusterRadiusThreshold;
    }
    
    /**
     * Creates a ClusterPathPlanner with a default mock distance calculator.
     * 
     * @param maxFlightHeight the maximum height for drone flight
     * @param maxClustersPerDiet maximum number of clusters to create per diet type
     * @param clusterRadiusThreshold distance threshold for clustering
     */
    public ClusterPathPlanner(
            int maxFlightHeight,
            int maxClustersPerDiet,
            double clusterRadiusThreshold) {
        this.distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
        this.maxClustersPerDiet = maxClustersPerDiet;
        this.clusterRadiusThreshold = clusterRadiusThreshold;
    }
    
    /**
     * Plan an optimal path using clustering approach.
     * 
     * @param depot Starting and ending location
     * @param enclosuresList List of animal enclosures that need feeding
     * @param foodStoragesList List of food storage locations
     * @param batteryCapacity Maximum distance allowed for this path
     * @return List of locations to visit in order, starting and ending with depot
     */
    public List<Location> planPath(
            Location depot,
            List<? extends Location> enclosuresList,
            List<? extends Location> foodStoragesList,
            double batteryCapacity) {
        
        // Cast to more specific types
        List<AnimalEnclosure> enclosures = castToAnimalEnclosures(enclosuresList);
        List<FoodStorage> foodStorages = castToFoodStorages(foodStoragesList);
        
        // Filter out already fed enclosures
        enclosures = enclosures.stream()
                .filter(e -> !e.isFed())
                .collect(Collectors.toList());
        
        // If no enclosures to feed, return just the depot
        if (enclosures.isEmpty()) {
            return Collections.singletonList(depot);
        }
        
        // Group enclosures by diet
        Map<Character, List<AnimalEnclosure>> enclosuresByDiet = groupEnclosuresByDiet(enclosures);
        
        // Create a map of food type to food storage
        Map<Character, FoodStorage> foodStorageMap = createFoodStorageMap(foodStorages);
        
        // Create clusters for each diet type
        Map<Character, List<Cluster>> clustersByDiet = new HashMap<>();
        for (char diet : enclosuresByDiet.keySet()) {
            List<AnimalEnclosure> dietEnclosures = enclosuresByDiet.get(diet);
            if (!dietEnclosures.isEmpty()) {
                List<Cluster> clusters = createClusters(dietEnclosures, diet);
                clustersByDiet.put(diet, clusters);
            }
        }
        
        // Sort clusters by score (importance/distance ratio)
        List<Cluster> allClusters = new ArrayList<>();
        for (List<Cluster> clusters : clustersByDiet.values()) {
            allClusters.addAll(clusters);
        }
        
        // Sort clusters by total importance
        allClusters.sort(Comparator.comparing(Cluster::getTotalImportance).reversed());
        
        // Build the path
        List<Location> completePath = new ArrayList<>();
        completePath.add(depot); // Start at depot
        
        double remainingBattery = batteryCapacity;
        Location currentLocation = depot;
        Character currentFoodType = null;
        
        // Process clusters in order of importance
        for (Cluster cluster : allClusters) {
            char diet = cluster.getDiet();
            FoodStorage foodStorage = foodStorageMap.get(diet);
            
            if (foodStorage == null) {
                continue; // Skip if no food storage for this diet
            }
            
            // If we need to change food type, visit the food storage
            if (currentFoodType == null || currentFoodType != diet) {
                // Calculate distance to food storage
                double distanceToFoodStorage = distanceCalculator.calculateDroneDistance(
                        currentLocation, foodStorage);
                
                // Check if we have enough battery to reach food storage
                if (distanceToFoodStorage > remainingBattery) {
                    // Not enough battery, return to depot
                    break;
                }
                
                // Move to food storage
                completePath.add(foodStorage);
                remainingBattery -= distanceToFoodStorage;
                currentLocation = foodStorage;
                currentFoodType = diet;
            }
            
            // Generate an optimal path through the cluster's enclosures
            List<Location> clusterPath = planClusterPath(
                    currentLocation,
                    cluster.getEnclosures(),
                    remainingBattery,
                    depot);
            
            // If the cluster path is empty, skip this cluster
            if (clusterPath.isEmpty() || clusterPath.size() <= 1) {
                continue;
            }
            
            // Skip the first location in cluster path as it's the current location
            for (int i = 1; i < clusterPath.size(); i++) {
                Location next = clusterPath.get(i);
                double distanceToNext = distanceCalculator.calculateDroneDistance(
                        currentLocation, next);
                
                // Check if we have enough battery
                if (distanceToNext > remainingBattery) {
                    // Not enough battery, can't proceed further
                    break;
                }
                
                // Add location to path
                completePath.add(next);
                remainingBattery -= distanceToNext;
                currentLocation = next;
                
                // Mark enclosure as fed if it's an animal enclosure
                if (next instanceof AnimalEnclosure) {
                    ((AnimalEnclosure) next).setFed(true);
                }
            }
            
            // Check if we need to return to depot
            double distanceToDepot = distanceCalculator.calculateDroneDistance(
                    currentLocation, depot);
            
            if (distanceToDepot > remainingBattery) {
                // Not enough battery to return to depot, path is invalid
                // Return just the depot to indicate an invalid path
                return Collections.singletonList(depot);
            }
        }
        
        // Return to depot if not already there
        if (!currentLocation.equals(depot)) {
            completePath.add(depot);
        }
        
        return completePath;
    }
    
    /**
     * Creates clusters of enclosures based on proximity and diet type.
     * 
     * @param enclosures List of enclosures to cluster
     * @param diet Diet type of these enclosures
     * @return List of clusters
     */
    private List<Cluster> createClusters(List<AnimalEnclosure> enclosures, char diet) {
        if (enclosures.isEmpty()) {
            return Collections.emptyList();
        }
        
        // For small number of enclosures, create a single cluster
        if (enclosures.size() <= 10) {
            Cluster singleCluster = new Cluster(diet);
            enclosures.forEach(singleCluster::addEnclosure);
            return Collections.singletonList(singleCluster);
        }
        
        // Use K-means clustering
        List<Cluster> clusters = initializeClusters(enclosures, diet);
        
        // Run K-means for a fixed number of iterations
        boolean changed = true;
        int maxIterations = 10;
        int iteration = 0;
        
        while (changed && iteration < maxIterations) {
            changed = assignEnclosuresToClusters(enclosures, clusters);
            updateClusterCenters(clusters);
            iteration++;
        }
        
        // Filter out empty clusters
        return clusters.stream()
                .filter(c -> !c.getEnclosures().isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Initialize clusters with random enclosures as centers.
     * 
     * @param enclosures List of enclosures
     * @param diet Diet type
     * @return List of initialized clusters
     */
    private List<Cluster> initializeClusters(List<AnimalEnclosure> enclosures, char diet) {
        int numClusters = Math.min(maxClustersPerDiet, enclosures.size());
        List<Cluster> clusters = new ArrayList<>(numClusters);
        
        // Shuffle enclosures to randomize initial centers
        List<AnimalEnclosure> shuffled = new ArrayList<>(enclosures);
        Collections.shuffle(shuffled);
        
        // Create clusters with initial centers
        for (int i = 0; i < numClusters; i++) {
            AnimalEnclosure center = shuffled.get(i);
            Cluster cluster = new Cluster(diet);
            cluster.setCenterX(center.getX());
            cluster.setCenterY(center.getY());
            clusters.add(cluster);
        }
        
        return clusters;
    }
    
    /**
     * Assign enclosures to the nearest cluster.
     * 
     * @param enclosures List of all enclosures
     * @param clusters List of clusters
     * @return true if any enclosure changed cluster, false otherwise
     */
    private boolean assignEnclosuresToClusters(List<AnimalEnclosure> enclosures, List<Cluster> clusters) {
        // Clear current assignments
        for (Cluster cluster : clusters) {
            cluster.clearEnclosures();
        }
        
        boolean anyChanged = false;
        
        // Assign each enclosure to the nearest cluster
        for (AnimalEnclosure enclosure : enclosures) {
            Cluster nearestCluster = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Cluster cluster : clusters) {
                double distance = calculateDistance(
                        enclosure.getX(), enclosure.getY(),
                        cluster.getCenterX(), cluster.getCenterY());
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCluster = cluster;
                }
            }
            
            if (nearestCluster != null) {
                nearestCluster.addEnclosure(enclosure);
                anyChanged = true;
            }
        }
        
        return anyChanged;
    }
    
    /**
     * Update cluster centers based on the average position of enclosures.
     * 
     * @param clusters List of clusters to update
     */
    private void updateClusterCenters(List<Cluster> clusters) {
        for (Cluster cluster : clusters) {
            List<AnimalEnclosure> clusterEnclosures = cluster.getEnclosures();
            
            if (clusterEnclosures.isEmpty()) {
                continue;
            }
            
            // Calculate average position
            double sumX = 0;
            double sumY = 0;
            
            for (AnimalEnclosure enclosure : clusterEnclosures) {
                sumX += enclosure.getX();
                sumY += enclosure.getY();
            }
            
            int centerX = (int) (sumX / clusterEnclosures.size());
            int centerY = (int) (sumY / clusterEnclosures.size());
            
            cluster.setCenterX(centerX);
            cluster.setCenterY(centerY);
        }
    }
    
    /**
     * Plan an optimal path through a cluster's enclosures.
     * Uses a nearest-neighbor approach within the cluster.
     * 
     * @param startLocation Starting location
     * @param enclosures Enclosures in the cluster
     * @param remainingBattery Remaining battery capacity
     * @param depot Depot location (for battery check)
     * @return Optimal path through the cluster
     */
    private List<Location> planClusterPath(
            Location startLocation,
            List<AnimalEnclosure> enclosures,
            double remainingBattery,
            Location depot) {
        
        List<Location> path = new ArrayList<>();
        path.add(startLocation);
        
        List<AnimalEnclosure> remainingEnclosures = new ArrayList<>(enclosures);
        Location currentLocation = startLocation;
        double batteryLeft = remainingBattery;
        
        while (!remainingEnclosures.isEmpty()) {
            // Find the nearest enclosure
            AnimalEnclosure nearest = null;
            double minDistance = Double.MAX_VALUE;
            
            for (AnimalEnclosure enclosure : remainingEnclosures) {
                double distance = distanceCalculator.calculateDroneDistance(
                        currentLocation, enclosure);
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = enclosure;
                }
            }
            
            if (nearest == null) {
                break;
            }
            
            // Calculate distance to next enclosure
            double distanceToNext = distanceCalculator.calculateDroneDistance(
                    currentLocation, nearest);
            
            // Calculate distance from next enclosure to depot
            double distanceToDepot = distanceCalculator.calculateDroneDistance(
                    nearest, depot);
            
            // Check if we have enough battery to visit next enclosure and return to depot
            if (distanceToNext + distanceToDepot > batteryLeft) {
                break; // Not enough battery, end the path
            }
            
            // Visit the enclosure
            path.add(nearest);
            batteryLeft -= distanceToNext;
            currentLocation = nearest;
            remainingEnclosures.remove(nearest);
        }
        
        return path;
    }
    
    /**
     * Calculate Euclidean distance between two points.
     */
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    /**
     * Group enclosures by their diet type.
     */
    private Map<Character, List<AnimalEnclosure>> groupEnclosuresByDiet(
            List<AnimalEnclosure> enclosures) {
        
        Map<Character, List<AnimalEnclosure>> enclosuresByDiet = new HashMap<>();
        
        for (AnimalEnclosure enclosure : enclosures) {
            char diet = enclosure.getDiet();
            enclosuresByDiet.computeIfAbsent(diet, k -> new ArrayList<>())
                    .add(enclosure);
        }
        
        return enclosuresByDiet;
    }
    
    /**
     * Create a map of food type to food storage.
     * If multiple storages exist for a food type, selects the first one.
     */
    private Map<Character, FoodStorage> createFoodStorageMap(List<FoodStorage> foodStorages) {
        Map<Character, FoodStorage> foodStorageMap = new HashMap<>();
        
        for (FoodStorage storage : foodStorages) {
            char foodType = storage.getFoodType();
            if (!foodStorageMap.containsKey(foodType)) {
                foodStorageMap.put(foodType, storage);
            }
        }
        
        return foodStorageMap;
    }
    
    /**
     * Safely cast a list of Locations to a list of AnimalEnclosures.
     */
    private List<AnimalEnclosure> castToAnimalEnclosures(List<? extends Location> locations) {
        List<AnimalEnclosure> result = new ArrayList<>();
        for (Location location : locations) {
            if (location instanceof AnimalEnclosure) {
                result.add((AnimalEnclosure) location);
            }
        }
        return result;
    }
    
    /**
     * Safely cast a list of Locations to a list of FoodStorages.
     */
    private List<FoodStorage> castToFoodStorages(List<? extends Location> locations) {
        List<FoodStorage> result = new ArrayList<>();
        for (Location location : locations) {
            if (location instanceof FoodStorage) {
                result.add((FoodStorage) location);
            }
        }
        return result;
    }
    
    /**
     * Inner class representing a cluster of enclosures.
     */
    private static class Cluster {
        private final char diet;
        private final List<AnimalEnclosure> enclosures = new ArrayList<>();
        private int centerX;
        private int centerY;
        
        public Cluster(char diet) {
            this.diet = diet;
        }
        
        public void addEnclosure(AnimalEnclosure enclosure) {
            enclosures.add(enclosure);
        }
        
        public void clearEnclosures() {
            enclosures.clear();
        }
        
        public List<AnimalEnclosure> getEnclosures() {
            return enclosures;
        }
        
        public char getDiet() {
            return diet;
        }
        
        public int getCenterX() {
            return centerX;
        }
        
        public int getCenterY() {
            return centerY;
        }
        
        public void setCenterX(int centerX) {
            this.centerX = centerX;
        }
        
        public void setCenterY(int centerY) {
            this.centerY = centerY;
        }
        
        public double getTotalImportance() {
            return enclosures.stream()
                    .mapToDouble(AnimalEnclosure::getImportance)
                    .sum();
        }
    }
}