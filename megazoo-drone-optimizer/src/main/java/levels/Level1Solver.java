package main.java.levels;

import core.algorithm.GreedyPathPlanner;
import core.algorithm.ScoreCalculator;
import core.mock.MockDistanceCalculator;
import core.models.AnimalEnclosure;
import core.models.Depot;
import core.models.FoodStorage;
import core.models.Location;
import core.services.ZooMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Solver implementation for Level 1.
 * 
 * Level 1 has these specific constraints:
 * - Size: 100x100x50m
 * - 20 enclosures
 * - No battery swaps (unlimited battery with 999,999m capacity)
 * - No deadzones
 * - 1 food storage of each diet type
 */
public class Level1Solver extends LevelSolver {
    
    /**
     * Constructor for Level1Solver with default parameters.
     * 
     * @param zooMap The map of the zoo
     */
    public Level1Solver(ZooMap zooMap) {
        // Level 1 has 999,999m battery capacity and 0 battery swaps
        super(zooMap, 999999, 0);
    }
    
    /**
     * Constructor with explicit parameters for testing or custom configurations.
     * 
     * @param zooMap The map of the zoo
     * @param batteryCapacity Custom battery capacity
     */
    public Level1Solver(ZooMap zooMap, double batteryCapacity) {
        super(zooMap, batteryCapacity, 0);
    }
    
    /**
     * Level 1 optimization strategy:
     * - Since battery is effectively unlimited, we can visit all enclosures in a single run
     * - We use a greedy approach to find the most efficient path
     * - No need for battery swap optimization
     * 
     * @param depot The depot location
     * @param enclosures List of animal enclosures
     * @param foodStorages List of food storages
     * @return A list containing a single path that visits all enclosures efficiently
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
        
        // For Level 1, we can simply use the greedy path planner to find a single optimal path
        List<Location> singlePath = pathPlanner.planPath(
                depot,
                enclosures,
                foodStorages,
                batteryCapacity
        );
        
        // Calculate distance of path to ensure it's within battery capacity
        MockDistanceCalculator distanceCalc = new MockDistanceCalculator(maxFlightHeight);
        double pathDistance = distanceCalc.calculatePathDistance(singlePath);
        
        // If path exceeds battery capacity (shouldn't happen in Level 1 but good to check)
        if (pathDistance > batteryCapacity) {
            System.out.println("Warning: Path distance " + pathDistance + 
                    " exceeds battery capacity " + batteryCapacity);
            
            // In a real implementation, we might try to optimize further here
        }
        
        // For Level 1, we return a list with just the single path
        return Collections.singletonList(singlePath);
    }
    
    /**
     * Additional method specific to Level 1:
     * Gets the fed enclosures in the single path solution.
     * 
     * @return List of enclosures that were fed in the solution
     */
    public List<AnimalEnclosure> getFedEnclosures() {
        List<List<Location>> paths = solve();
        
        if (paths.isEmpty()) {
            return Collections.emptyList();
        }
        
        // For Level 1, we only have one path
        List<Location> path = paths.get(0);
        return extractFedEnclosures(path);
    }
    
    /**
     * Get detailed score information for the Level 1 solution.
     * 
     * @return A string with score details
     */
    public String getScoreDetails() {
        List<List<Location>> paths = solve();
        
        if (paths.isEmpty()) {
            return "No valid path found";
        }
        
        List<Location> path = paths.get(0);
        List<AnimalEnclosure> fedEnclosures = extractFedEnclosures(path);
        
        MockDistanceCalculator distanceCalc = new MockDistanceCalculator(maxFlightHeight);
        double totalDistance = distanceCalc.calculatePathDistance(path);
        
        double totalImportance = 0;
        for (AnimalEnclosure enclosure : fedEnclosures) {
            totalImportance += enclosure.getImportance();
        }
        
        double score = totalImportance * 1000 - totalDistance;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Level 1 Score Details:\n");
        sb.append("Total enclosures fed: ").append(fedEnclosures.size()).append("\n");
        sb.append("Total importance: ").append(totalImportance).append("\n");
        sb.append("Total distance: ").append(totalDistance).append("m\n");
        sb.append("Final Score: ").append(score).append(" points");
        
        return sb.toString();
    }
}