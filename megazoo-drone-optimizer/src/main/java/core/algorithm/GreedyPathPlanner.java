package core.algorithm;

import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.FoodStorage;
import core.models.Location;
import main.java.core.services.DistanceCalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A greedy implementation for drone path planning.
 * This planner uses a heuristic approach that prioritizes enclosures with the highest
 * importance-to-distance ratio, grouped by food type to minimize food storage visits.
 */
public class GreedyPathPlanner {
    
    private final DistanceCalculator distanceCalculator;
    
    /**
     * Creates a GreedyPathPlanner with a specific distance calculator.
     * 
     * @param distanceCalculator the calculator to use for distance measurements
     */
    public GreedyPathPlanner(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }
    
    /**
     * Creates a GreedyPathPlanner with a default mock distance calculator.
     * 
     * @param maxFlightHeight the maximum height for drone flight
     */
    public GreedyPathPlanner(int maxFlightHeight) {
        this.distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
    }
    
    /**
     * Plan an optimal path to feed animals within battery constraints
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
        
        // Cast to more specific types for easier handling
        List<AnimalEnclosure> enclosures = castToAnimalEnclosures(enclosuresList);
        List<FoodStorage> foodStorages = castToFoodStorages(foodStoragesList);
        
        // Filter out already fed enclosures
        enclosures = enclosures.stream()
                .filter(e -> !e.isFed())
                .collect(Collectors.toList());
        
        // Group enclosures by diet/food type
        Map<Character, List<AnimalEnclosure>> enclosuresByDiet = groupEnclosuresByDiet(enclosures);
        
        // Create a map of food type to food storage
        Map<Character, FoodStorage> foodStorageMap = createFoodStorageMap(foodStorages);
        
        // Build the optimal path
        List<Location> path = new ArrayList<>();
        path.add(depot); // Start at depot
        
        double remainingBattery = batteryCapacity;
        Location currentLocation = depot;
        Character currentFoodType = null;
        
        // Process each diet group
        for (char diet : new char[]{'c', 'h', 'o'}) {
            if (!enclosuresByDiet.containsKey(diet) || enclosuresByDiet.get(diet).isEmpty()) {
                continue; // Skip if no enclosures of this diet
            }
            
            // Get the food storage for this diet
            FoodStorage foodStorage = foodStorageMap.get(diet);
            if (foodStorage == null) {
                continue; // Skip if no food storage for this diet
            }
            
            // Calculate distance to food storage
            double distanceToFoodStorage = ((MockDistanceCalculator) distanceCalculator)
                    .calculateDroneDistance(currentLocation, foodStorage);
            
            // Check if we have enough battery to reach food storage
            if (distanceToFoodStorage > remainingBattery) {
                break; // Not enough battery to continue
            }
            
            // Move to food storage
            path.add(foodStorage);
            remainingBattery -= distanceToFoodStorage;
            currentLocation = foodStorage;
            currentFoodType = foodStorage.getFoodType();
            
            // Score and sort enclosures by importance/distance ratio
            List<ScoredEnclosure> scoredEnclosures = scoreEnclosures(
                    enclosuresByDiet.get(diet), currentLocation);
            
            // Visit enclosures in order of highest score
            for (ScoredEnclosure scoredEnclosure : scoredEnclosures) {
                AnimalEnclosure enclosure = scoredEnclosure.getEnclosure();
                
                // Calculate distance to this enclosure
                double distanceToEnclosure = ((MockDistanceCalculator) distanceCalculator)
                        .calculateDroneDistance(currentLocation, enclosure);
                
                // Calculate distance from enclosure back to depot
                double distanceToDepot = ((MockDistanceCalculator) distanceCalculator)
                        .calculateDroneDistance(enclosure, depot);
                
                // Check if we have enough battery to visit enclosure and return to depot
                if (distanceToEnclosure + distanceToDepot > remainingBattery) {
                    continue; // Skip this enclosure, not enough battery
                }
                
                // Visit enclosure
                path.add(enclosure);
                remainingBattery -= distanceToEnclosure;
                currentLocation = enclosure;
                
                // Mark enclosure as fed
                enclosure.setFed(true);
            }
        }
        
        // Return to depot if not already there
        if (!currentLocation.equals(depot)) {
            path.add(depot);
        }
        
        return path;
    }
    
    /**
     * Calculate the estimated score for a planned path.
     * 
     * @param path The planned path
     * @param fedEnclosuresList Enclosures that will be fed in this path
     * @return Estimated score according to formula: (sum of importance * 1000) - distance
     */
    public double estimatePathScore(List<Location> path, List<? extends Location> fedEnclosuresList) {
        List<AnimalEnclosure> fedEnclosures = castToAnimalEnclosures(fedEnclosuresList);
        
        // Calculate total importance
        double totalImportance = fedEnclosures.stream()
                .mapToDouble(AnimalEnclosure::getImportance)
                .sum();
        
        // Calculate total distance
        double totalDistance = 0;
        Location previous = null;
        for (Location current : path) {
            if (previous != null) {
                totalDistance += ((MockDistanceCalculator) distanceCalculator)
                        .calculateDroneDistance(previous, current);
            }
            previous = current;
        }
        
        // Score = sum(importance * 1000) - distance
        return (totalImportance * 1000) - totalDistance;
    }
    
    /**
     * Helper class to associate an enclosure with its score.
     */
    private static class ScoredEnclosure {
        private final AnimalEnclosure enclosure;
        private final double score;
        
        public ScoredEnclosure(AnimalEnclosure enclosure, double score) {
            this.enclosure = enclosure;
            this.score = score;
        }
        
        public AnimalEnclosure getEnclosure() {
            return enclosure;
        }
        
        public double getScore() {
            return score;
        }
    }
    
    /**
     * Score enclosures based on importance/distance ratio.
     * 
     * @param enclosures the enclosures to score
     * @param currentLocation the current location to calculate distances from
     * @return list of scored enclosures sorted by descending score
     */
    private List<ScoredEnclosure> scoreEnclosures(
            List<AnimalEnclosure> enclosures, Location currentLocation) {
        
        List<ScoredEnclosure> scoredEnclosures = new ArrayList<>();
        
        for (AnimalEnclosure enclosure : enclosures) {
            double distance = ((MockDistanceCalculator) distanceCalculator)
                    .calculateDroneDistance(currentLocation, enclosure);
            
            // Avoid division by zero
            if (distance < 1) {
                distance = 1;
            }
            
            // Score = importance / distance
            double score = enclosure.getImportance() / distance;
            scoredEnclosures.add(new ScoredEnclosure(enclosure, score));
        }
        
        // Sort by descending score
        scoredEnclosures.sort(Comparator.comparing(ScoredEnclosure::getScore).reversed());
        
        return scoredEnclosures;
    }
    
    /**
     * Group enclosures by their diet type.
     * 
     * @param enclosures the enclosures to group
     * @return map of diet type to list of enclosures
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
     * 
     * @param foodStorages the list of food storages
     * @return map of food type to food storage
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
     * 
     * @param locations list of locations to cast
     * @return list of animal enclosures
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
     * 
     * @param locations list of locations to cast
     * @return list of food storages
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
}