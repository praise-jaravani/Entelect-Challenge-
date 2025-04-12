package main.java.levels;

import main.java.core.algorithm.ClusterPathPlanner;
import main.java.core.algorithm.DeadzoneAvoidancePathPlanner;
import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.Deadzone;
import main.java.core.models.Depot;
import main.java.core.models.FoodStorage;
import main.java.core.models.Location;
import main.java.core.services.ZooMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Solver implementation for Level 4.
 * 
 * Level 4 has these specific constraints:
 * - Size: 2500x2500x50m
 * - 15000 enclosures
 * - 250 battery swaps allowed
 * - Battery capacity: 9250m
 * - 15 deadzones
 * - 10 food storages of each diet type
 */
public class Level4Solver extends LevelSolver {
    
    private final ClusterPathPlanner clusterPlanner;
    private final DeadzoneAvoidancePathPlanner deadzoneAvoidancePlanner;
    
    // Configuration for Level 4
    private final int maxClustersPerDiet = 25; // Number of clusters to create per diet type
    private final double clusterRadiusThreshold = 250.0; // Size threshold for clusters (in meters)
    private final int batchSize = 20; // Number of paths to generate in each batch
    
    /**
     * Constructor for Level4Solver with default parameters.
     * 
     * @param zooMap The map of the zoo
     */
    public Level4Solver(ZooMap zooMap) {
        // Level 4 has 9250m battery capacity and 250 battery swaps
        super(zooMap, 9250, 250);
        
        // Initialize specialized planners for Level 4
        MockDistanceCalculator distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
        
        this.clusterPlanner = new ClusterPathPlanner(
                distanceCalculator,
                maxClustersPerDiet,
                clusterRadiusThreshold
        );
        
        this.deadzoneAvoidancePlanner = new DeadzoneAvoidancePathPlanner(
                distanceCalculator, 
                zooMap.getAllDeadzones()
        );
    }
    
    /**
     * Constructor with explicit parameters for testing or custom configurations.
     * 
     * @param zooMap The map of the zoo
     * @param batteryCapacity Custom battery capacity
     * @param maxBatterySwaps Custom number of battery swaps
     */
    public Level4Solver(ZooMap zooMap, double batteryCapacity, int maxBatterySwaps) {
        super(zooMap, batteryCapacity, maxBatterySwaps);
        
        // Initialize specialized planners for Level 4
        MockDistanceCalculator distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
        
        this.clusterPlanner = new ClusterPathPlanner(
                distanceCalculator,
                maxClustersPerDiet,
                clusterRadiusThreshold
        );
        
        this.deadzoneAvoidancePlanner = new DeadzoneAvoidancePathPlanner(
                distanceCalculator, 
                zooMap.getAllDeadzones()
        );
    }
    
    /**
     * Level 4 optimization strategy:
     * - Uses clustering to handle the massive number of enclosures
     * - Combines deadzone avoidance with cluster-based planning
     * - Processes enclosures in batches to manage complexity
     * - Prioritizes high-importance clusters
     * 
     * @param depot The depot location
     * @param enclosures List of animal enclosures
     * @param foodStorages List of food storages
     * @return List of optimized paths that avoid deadzones and respect battery constraints
     */
    @Override
    protected List<List<Location>> optimizeForLevel(
            Depot depot,
            List<AnimalEnclosure> enclosures,
            List<FoodStorage> foodStorages) {
        
        // Reset fed status of all enclosures
        for (AnimalEnclosure enclosure : enclosures) {
            enclosure.setFed(false);
        }
        
        System.out.println("Level 4: Planning paths for " + enclosures.size() + " enclosures");
        System.out.println("Using " + maxBatterySwaps + " battery swaps with " + batteryCapacity + "m capacity");
        
        // Get deadzones for reference
        List<Deadzone> deadzones = zooMap.getAllDeadzones();
        System.out.println("Planning with " + deadzones.size() + " deadzones");
        
        // Create a master list for all paths
        List<List<Location>> allPaths = new ArrayList<>();
        
        // Sort enclosures by importance for prioritization
        List<AnimalEnclosure> sortedEnclosures = new ArrayList<>(enclosures);
        sortedEnclosures.sort(Comparator.comparing(AnimalEnclosure::getImportance).reversed());
        
        // Group enclosures by diet for more efficient planning
        Map<Character, List<AnimalEnclosure>> enclosuresByDiet = groupEnclosuresByDiet(sortedEnclosures);
        
        // Process diet groups in parallel batches
        int batterySwapsUsed = 0;
        boolean continueProcessing = true;
        
        while (continueProcessing && batterySwapsUsed < maxBatterySwaps) {
            continueProcessing = false;
            
            // Process each diet type
            for (char diet : new char[]{'c', 'h', 'o'}) {
                if (!enclosuresByDiet.containsKey(diet)) continue;
                
                List<AnimalEnclosure> dietEnclosures = enclosuresByDiet.get(diet);
                
                // Filter out already fed enclosures
                dietEnclosures = dietEnclosures.stream()
                        .filter(e -> !e.isFed())
                        .collect(Collectors.toList());
                
                if (dietEnclosures.isEmpty()) continue;
                
                continueProcessing = true; // We still have enclosures to process
                
                // Only process a batch of enclosures at a time to manage complexity
                List<AnimalEnclosure> batchEnclosures = dietEnclosures.stream()
                        .limit(Math.min(batchSize * 50, dietEnclosures.size()))
                        .collect(Collectors.toList());
                
                // Create paths for this diet group
                int availableSwaps = Math.min(batchSize, maxBatterySwaps - batterySwapsUsed);
                List<List<Location>> dietPaths = createPathsForDietGroup(
                        depot, batchEnclosures, foodStorages, availableSwaps);
                
                // Update battery swaps used
                batterySwapsUsed += dietPaths.size();
                
                // Add paths to master list
                allPaths.addAll(dietPaths);
                
                // Break if we've used all available battery swaps
                if (batterySwapsUsed >= maxBatterySwaps) {
                    break;
                }
            }
        }
        
        // Calculate and display statistics
        int totalFed = countFedEnclosures(enclosures);
        System.out.println("Level 4 solution: Fed " + totalFed + " of " + enclosures.size() + 
                " enclosures using " + allPaths.size() + " battery swaps");
        
        return allPaths;
    }
    
    /**
     * Create paths for a group of enclosures with the same diet type.
     * 
     * @param depot The depot location
     * @param enclosures List of enclosures (same diet type)
     * @param allFoodStorages List of all food storages
     * @param maxPaths Maximum number of paths to create
     * @return List of paths for this diet group
     */
    private List<List<Location>> createPathsForDietGroup(
            Depot depot,
            List<AnimalEnclosure> enclosures,
            List<FoodStorage> allFoodStorages,
            int maxPaths) {
        
        List<List<Location>> paths = new ArrayList<>();
        
        // If no enclosures or no paths allowed, return empty list
        if (enclosures.isEmpty() || maxPaths <= 0) {
            return paths;
        }
        
        // Get the diet type for this group
        char diet = enclosures.get(0).getDiet();
        
        // Filter food storages to only include those matching the diet
        List<FoodStorage> matchingFoodStorages = allFoodStorages.stream()
                .filter(fs -> fs.getFoodType() == diet)
                .collect(Collectors.toList());
        
        if (matchingFoodStorages.isEmpty()) {
            return paths; // No matching food storages
        }
        
        // Create a copy of enclosures to track which ones are fed
        List<AnimalEnclosure> remainingEnclosures = new ArrayList<>(enclosures);
        
        // Generate paths until we've used all allowed paths or fed all enclosures
        for (int i = 0; i < maxPaths && !remainingEnclosures.isEmpty(); i++) {
            // First, use the cluster planner to create an efficient path
            List<Location> clusterPath = clusterPlanner.planPath(
                    depot,
                    remainingEnclosures,
                    matchingFoodStorages,
                    batteryCapacity
            );
            
            // If the path is just the depot or empty, we can't feed any more enclosures
            if (clusterPath.size() <= 2) {
                break;
            }
            
            // Next, check and adjust for deadzones
            List<Location> safePath = deadzoneAvoidancePlanner.planPath(
                    depot,
                    extractLocations(clusterPath),  // Extract locations that aren't depot
                    matchingFoodStorages,
                    batteryCapacity
            );
            
            // If the safe path is just the depot or empty, we can't feed any more enclosures
            if (safePath.size() <= 2) {
                break;
            }
            
            // Add the path to our collection
            paths.add(safePath);
            
            // Update the list of remaining enclosures
            updateRemainingEnclosures(safePath, remainingEnclosures);
        }
        
        return paths;
    }
    
    /**
     * Extract all non-depot locations from a path.
     * 
     * @param path The original path
     * @return List of locations excluding the depot
     */
    private List<Location> extractLocations(List<Location> path) {
        // Skip first and last elements (depot)
        if (path.size() <= 2) {
            return new ArrayList<>();
        }
        
        return path.subList(1, path.size() - 1);
    }
    
    /**
     * Updates the list of remaining enclosures by removing those that were fed in the path.
     * 
     * @param path The path that was just completed
     * @param remainingEnclosures The list of enclosures that still need to be fed
     */
    private void updateRemainingEnclosures(List<Location> path, List<AnimalEnclosure> remainingEnclosures) {
        // Track current food type
        Character currentFoodType = null;
        
        for (Location location : path) {
            if (location instanceof FoodStorage) {
                FoodStorage storage = (FoodStorage) location;
                currentFoodType = storage.getFoodType();
            } else if (location instanceof AnimalEnclosure) {
                AnimalEnclosure enclosure = (AnimalEnclosure) location;
                
                // Check if the enclosure was fed (right food type)
                if (currentFoodType != null && currentFoodType == enclosure.getDiet()) {
                    remainingEnclosures.remove(enclosure);
                    enclosure.setFed(true);
                }
            }
        }
    }
    
    /**
     * Group enclosures by their diet type.
     * 
     * @param enclosures List of enclosures to group
     * @return Map of diet type to list of enclosures
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
     * Count how many enclosures have been fed.
     * 
     * @param enclosures List of all enclosures
     * @return Number of enclosures that have been fed
     */
    private int countFedEnclosures(List<AnimalEnclosure> enclosures) {
        return (int) enclosures.stream().filter(AnimalEnclosure::isFed).count();
    }
    
    /**
     * Get detailed score information for the Level 4 solution.
     * 
     * @return A string with score details and statistics
     */
    public String getScoreDetails() {
        List<List<Location>> paths = solve();
        
        if (paths.isEmpty()) {
            return "No valid paths found";
        }
        
        double totalScore = 0;
        int totalEnclosuresFed = 0;
        double totalImportance = 0;
        double totalDistance = 0;
        
        Map<Character, Integer> fedByDiet = new HashMap<>();
        fedByDiet.put('c', 0);
        fedByDiet.put('h', 0);
        fedByDiet.put('o', 0);
        
        MockDistanceCalculator distanceCalc = new MockDistanceCalculator(maxFlightHeight);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Level 4 Score Details:\n");
        
        // For each battery swap, calculate the score
        for (int i = 0; i < paths.size(); i++) {
            List<Location> path = paths.get(i);
            List<AnimalEnclosure> fedEnclosures = extractFedEnclosures(path);
            
            double pathDistance = distanceCalc.calculatePathDistance(path);
            
            double pathImportance = 0;
            for (AnimalEnclosure enclosure : fedEnclosures) {
                pathImportance += enclosure.getImportance();
                fedByDiet.put(enclosure.getDiet(), fedByDiet.get(enclosure.getDiet()) + 1);
            }
            
            double pathScore = pathImportance * 1000 - pathDistance;
            
            // For Level 4, we have too many paths to show details for each one
            // Just accumulate the totals
            totalScore += pathScore;
            totalEnclosuresFed += fedEnclosures.size();
            totalImportance += pathImportance;
            totalDistance += pathDistance;
        }
        
        sb.append("\nSummary:\n");
        sb.append("Total paths: ").append(paths.size()).append(" (of ").append(maxBatterySwaps).append(" available)\n");
        sb.append("Total enclosures fed: ").append(totalEnclosuresFed).append(" of ").append(zooMap.getAllEnclosures().size()).append("\n");
        sb.append("Diet distribution:\n");
        sb.append("  Carnivores: ").append(fedByDiet.get('c')).append("\n");
        sb.append("  Herbivores: ").append(fedByDiet.get('h')).append("\n");
        sb.append("  Omnivores: ").append(fedByDiet.get('o')).append("\n");
        sb.append("Total importance: ").append(totalImportance).append("\n");
        sb.append("Total distance: ").append(totalDistance).append("m\n");
        sb.append("Final Score: ").append(totalScore).append(" points");
        
        return sb.toString();
    }
    
    /**
     * Get efficiency metrics for the Level 4 solution.
     * 
     * @return A string with efficiency metrics
     */
    public String getEfficiencyMetrics() {
        List<List<Location>> paths = solve();
        
        if (paths.isEmpty()) {
            return "No valid paths found";
        }
        
        // Calculate metrics
        double totalDistance = 0;
        int totalEnclosuresFed = 0;
        double totalImportance = 0;
        
        MockDistanceCalculator distanceCalc = new MockDistanceCalculator(maxFlightHeight);
        
        for (List<Location> path : paths) {
            List<AnimalEnclosure> fedEnclosures = extractFedEnclosures(path);
            totalDistance += distanceCalc.calculatePathDistance(path);
            totalEnclosuresFed += fedEnclosures.size();
            
            for (AnimalEnclosure enclosure : fedEnclosures) {
                totalImportance += enclosure.getImportance();
            }
        }
        
        // Calculate derived metrics
        double enclosuresPerPath = (double) totalEnclosuresFed / paths.size();
        double distancePerEnclosure = totalDistance / totalEnclosuresFed;
        double importancePerDistance = totalImportance / totalDistance;
        double importancePerPath = totalImportance / paths.size();
        double batteryEfficiency = totalDistance / (paths.size() * batteryCapacity) * 100;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Level 4 Efficiency Metrics:\n");
        sb.append("Enclosures fed per path: ").append(String.format("%.2f", enclosuresPerPath)).append("\n");
        sb.append("Distance per enclosure: ").append(String.format("%.2f", distancePerEnclosure)).append("m\n");
        sb.append("Importance per distance: ").append(String.format("%.4f", importancePerDistance)).append(" points/m\n");
        sb.append("Importance per path: ").append(String.format("%.2f", importancePerPath)).append(" points\n");
        sb.append("Battery efficiency: ").append(String.format("%.2f", batteryEfficiency)).append("%");
        
        return sb.toString();
    }
}