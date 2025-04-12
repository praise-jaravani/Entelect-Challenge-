package main.java.levels;

import main.java.core.algorithm.GreedyPathPlanner;
import main.java.core.algorithm.RouteOptimizer;
import main.java.core.algorithm.ScoreCalculator;
import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.Depot;
import main.java.core.models.FoodStorage;
import main.java.core.models.Location;
import main.java.core.services.ZooMap;
import main.java.core.utils.OutputFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Solver implementation for Level 2.
 * 
 * Level 2 has these specific constraints:
 * - Size: 250x250x50m
 * - 100 enclosures
 * - 10 battery swaps allowed
 * - Battery capacity: 1125m
 * - No deadzones
 * - 3 food storages of each diet type
 */
public class Level2Solver extends LevelSolver {
    
    /**
     * Constructor for Level2Solver with default parameters.
     * 
     * @param zooMap The map of the zoo
     */
    public Level2Solver(ZooMap zooMap) {
        // Level 2 has 1125m battery capacity and 10 battery swaps
        super(zooMap, 1125, 10);
    }
    
    /**
     * Constructor with explicit parameters for testing or custom configurations.
     * 
     * @param zooMap The map of the zoo
     * @param batteryCapacity Custom battery capacity
     * @param maxBatterySwaps Custom number of battery swaps
     */
    public Level2Solver(ZooMap zooMap, double batteryCapacity, int maxBatterySwaps) {
        super(zooMap, batteryCapacity, maxBatterySwaps);
    }
    
    /**
     * Level 2 optimization strategy:
     * - Uses battery swaps since battery capacity is limited
     * - Plans multiple optimized routes to maximize feeding efficiency
     * - Focuses on high-importance enclosures first
     * 
     * @param depot The depot location
     * @param enclosures List of animal enclosures
     * @param foodStorages List of food storages
     * @return List of optimized paths that respect battery constraints
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
        
        // For Level 2, we need to use battery swaps effectively
        // First, use the RouteOptimizer to generate multiple paths
        return routeOptimizer.optimizeMultipleRoutes(
                depot,
                enclosures,
                foodStorages,
                batteryCapacity,
                maxBatterySwaps
        );
    }
    
    /**
     * Get detailed score information for the Level 2 solution.
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
        sb.append("Level 2 Score Details:\n");
        
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
     * Get a summary of how many enclosures of each diet type were fed.
     * 
     * @return A string with diet distribution information
     */
    public String getDietDistribution() {
        List<List<Location>> paths = solve();
        
        if (paths.isEmpty()) {
            return "No valid paths found";
        }
        
        int carnivoresFed = 0;
        int herbivoresFed = 0;
        int omnivoresFed = 0;
        
        for (List<Location> path : paths) {
            List<AnimalEnclosure> fedEnclosures = extractFedEnclosures(path);
            
            for (AnimalEnclosure enclosure : fedEnclosures) {
                char diet = enclosure.getDiet();
                if (diet == 'c') {
                    carnivoresFed++;
                } else if (diet == 'h') {
                    herbivoresFed++;
                } else if (diet == 'o') {
                    omnivoresFed++;
                }
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Diet Distribution:\n");
        sb.append("Carnivores fed: ").append(carnivoresFed).append("\n");
        sb.append("Herbivores fed: ").append(herbivoresFed).append("\n");
        sb.append("Omnivores fed: ").append(omnivoresFed).append("\n");
        sb.append("Total fed: ").append(carnivoresFed + herbivoresFed + omnivoresFed);
        
        return sb.toString();
    }
}