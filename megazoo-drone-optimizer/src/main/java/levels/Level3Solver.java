package main.java.levels;

import core.algorithm.DeadzoneAvoidancePathPlanner;
import core.algorithm.GreedyPathPlanner;
import core.algorithm.RouteOptimizer;
import core.algorithm.ScoreCalculator;
import core.mock.MockDistanceCalculator;
import core.models.AnimalEnclosure;
import core.models.Deadzone;
import core.models.Depot;
import core.models.FoodStorage;
import core.models.Location;
import core.services.ZooMap;
import core.utils.OutputFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Solver implementation for Level 3.
 * 
 * Level 3 has these specific constraints:
 * - Size: 700x700x50m
 * - 1000 enclosures
 * - 50 battery swaps allowed
 * - Battery capacity: 2750m
 * - 5 deadzones
 * - 5 food storages of each diet type
 */
public class Level3Solver extends LevelSolver {
    
    private final DeadzoneAvoidancePathPlanner deadzoneAvoidancePlanner;
    
    /**
     * Constructor for Level3Solver with default parameters.
     * 
     * @param zooMap The map of the zoo
     */
    public Level3Solver(ZooMap zooMap) {
        // Level 3 has 2750m battery capacity and 50 battery swaps
        super(zooMap, 2750, 50);
        
        // Initialize deadzone avoidance planner
        MockDistanceCalculator distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
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
    public Level3Solver(ZooMap zooMap, double batteryCapacity, int maxBatterySwaps) {
        super(zooMap, batteryCapacity, maxBatterySwaps);
        
        // Initialize deadzone avoidance planner
        MockDistanceCalculator distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
        this.deadzoneAvoidancePlanner = new DeadzoneAvoidancePathPlanner(
                distanceCalculator, 
                zooMap.getAllDeadzones()
        );
    }
    
    /**
     * Level 3 optimization strategy:
     * - Uses deadzone avoidance to safely navigate the zoo
     * - Plans multiple paths with battery swaps
     * - Optimizes for both importance and safety
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
        
        // Get deadzones for logging
        List<Deadzone> deadzones = zooMap.getAllDeadzones();
        System.out.println("Planning paths with " + deadzones.size() + " deadzones");
        
        // Create an empty list to store our paths
        List<List<Location>> allPaths = new ArrayList<>();
        
        // Create a copy of enclosures to track remaining ones
        List<AnimalEnclosure> remainingEnclosures = new ArrayList<>(enclosures);
        
        // Keep generating paths until we've used all battery swaps or fed all enclosures
        for (int i = 0; i < maxBatterySwaps && !remainingEnclosures.isEmpty(); i++) {
            // Use deadzone avoidance planner to create a safe path
            List<Location> safePath = deadzoneAvoidancePlanner.planPath(
                    depot,
                    remainingEnclosures,
                    foodStorages,
                    batteryCapacity
            );
            
            // If path is just the depot or empty, we can't feed any more enclosures
            if (safePath.size() <= 2) {
                break;
            }
            
            // Add the path to our collection
            allPaths.add(safePath);
            
            // Update the list of remaining enclosures
            updateRemainingEnclosures(safePath, remainingEnclosures);
            
            // Log progress
            System.out.println("Generated path " + (i + 1) + 
                    ", fed " + (enclosures.size() - remainingEnclosures.size()) + 
                    " of " + enclosures.size() + " enclosures");
        }
        
        return allPaths;
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
                }
            }
        }
    }
    
    /**
     * Get detailed score information for the Level 3 solution.
     * 
     * @return A string with score details
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
        
        MockDistanceCalculator distanceCalc = new MockDistanceCalculator(maxFlightHeight);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Level 3 Score Details:\n");
        
        // For each battery swap, calculate the score
        for (int i = 0; i < paths.size(); i++) {
            List<Location> path = paths.get(i);
            List<AnimalEnclosure> fedEnclosures = extractFedEnclosures(path);
            
            double pathDistance = distanceCalc.calculatePathDistance(path);
            
            double pathImportance = 0;
            for (AnimalEnclosure enclosure : fedEnclosures) {
                pathImportance += enclosure.getImportance();
            }
            
            double pathScore = pathImportance * 1000 - pathDistance;
            
            sb.append("Path ").append(i + 1).append(":\n");
            sb.append("  Enclosures fed: ").append(fedEnclosures.size()).append("\n");
            sb.append("  Importance: ").append(pathImportance).append("\n");
            sb.append("  Distance: ").append(pathDistance).append("m\n");
            sb.append("  Score: ").append(pathScore).append(" points\n");
            
            totalScore += pathScore;
            totalEnclosuresFed += fedEnclosures.size();
            totalImportance += pathImportance;
            totalDistance += pathDistance;
        }
        
        sb.append("\nSummary:\n");
        sb.append("Total paths: ").append(paths.size()).append(" (of ").append(maxBatterySwaps).append(" available)\n");
        sb.append("Total enclosures fed: ").append(totalEnclosuresFed).append("\n");
        sb.append("Total importance: ").append(totalImportance).append("\n");
        sb.append("Total distance: ").append(totalDistance).append("m\n");
        sb.append("Final Score: ").append(totalScore).append(" points");
        
        return sb.toString();
    }
    
    /**
     * Check if any paths in the solution intersect with deadzones.
     * 
     * @return true if any path intersects a deadzone, false otherwise
     */
    public boolean hasDeadzoneIntersections() {
        List<List<Location>> paths = solve();
        List<Deadzone> deadzones = zooMap.getAllDeadzones();
        
        for (List<Location> path : paths) {
            for (int i = 0; i < path.size() - 1; i++) {
                Location current = path.get(i);
                Location next = path.get(i + 1);
                
                for (Deadzone deadzone : deadzones) {
                    if (deadzone.intersectsLine(
                            current.getX(), current.getY(),
                            next.getX(), next.getY())) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}