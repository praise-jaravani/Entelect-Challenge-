package main.java.levels;

import core.algorithm.GreedyPathPlanner;
import core.algorithm.RouteOptimizer;
import core.algorithm.ScoreCalculator;
import core.mock.MockDistanceCalculator;
import core.models.AnimalEnclosure;
import core.models.Depot;
import core.models.FoodStorage;
import core.models.Location;
import core.services.ZooMap;
import core.utils.OutputFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class for all level solvers.
 * Provides common functionality for solving drone paths for different levels.
 */
public abstract class LevelSolver {
    
    // Core components used by all level solvers
    protected GreedyPathPlanner pathPlanner;
    protected RouteOptimizer routeOptimizer;
    protected ScoreCalculator scoreCalculator;
    protected OutputFormatter outputFormatter;
    
    // Zoo configuration
    protected ZooMap zooMap;
    protected int maxFlightHeight = 50; // Default max flight height
    protected double batteryCapacity;
    protected int maxBatterySwaps;
    
    /**
     * Initialize the level solver with the given zoo map and constraints.
     * 
     * @param zooMap The map of the zoo
     * @param batteryCapacity Maximum distance per battery charge
     * @param maxBatterySwaps Maximum number of battery swaps allowed
     */
    public LevelSolver(ZooMap zooMap, double batteryCapacity, int maxBatterySwaps) {
        this.zooMap = zooMap;
        this.batteryCapacity = batteryCapacity;
        this.maxBatterySwaps = maxBatterySwaps;
        this.maxFlightHeight = zooMap.getMaxHeight();
        
        // Initialize common components
        MockDistanceCalculator distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
        this.pathPlanner = new GreedyPathPlanner(distanceCalculator);
        this.routeOptimizer = new RouteOptimizer(distanceCalculator);
        this.scoreCalculator = new ScoreCalculator(distanceCalculator);
        this.outputFormatter = new OutputFormatter();
    }
    
    /**
     * Solve the path planning problem for this level.
     * 
     * @return List of optimized paths that respect battery constraints
     */
    public List<List<Location>> solve() {
        // Get zoo entities
        Depot depot = zooMap.getDepot();
        List<AnimalEnclosure> enclosures = zooMap.getAllEnclosures();
        List<FoodStorage> foodStorages = zooMap.getAllFoodStorages();
        
        // Call the level-specific optimization method
        return optimizeForLevel(depot, enclosures, foodStorages);
    }
    
    /**
     * Generate a submission string from the solved paths.
     * 
     * @return Formatted string ready for submission
     */
    public String generateSubmission() {
        List<List<Location>> paths = solve();
        return outputFormatter.formatForSubmission(paths);
    }
    
    /**
     * Calculate the total score for the solved paths.
     * 
     * @return The total score for all paths
     */
    public double calculateTotalScore() {
        List<List<Location>> paths = solve();
        
        // Extract fed enclosures for each path
        List<List<AnimalEnclosure>> fedEnclosuresList = new ArrayList<>();
        
        for (List<Location> path : paths) {
            List<AnimalEnclosure> fedEnclosures = extractFedEnclosures(path);
            fedEnclosuresList.add(fedEnclosures);
        }
        
        return scoreCalculator.calculateTotalScore(paths, fedEnclosuresList);
    }
    
    /**
     * Extract the enclosures that were fed in a path.
     * 
     * @param path The path to analyze
     * @return List of enclosures that were fed
     */
    protected List<AnimalEnclosure> extractFedEnclosures(List<Location> path) {
        List<AnimalEnclosure> fedEnclosures = new ArrayList<>();
        Character currentFoodType = null;
        
        for (Location location : path) {
            if (location instanceof FoodStorage) {
                FoodStorage storage = (FoodStorage) location;
                currentFoodType = storage.getFoodType();
            } else if (location instanceof AnimalEnclosure) {
                AnimalEnclosure enclosure = (AnimalEnclosure) location;
                
                // An enclosure is fed if the drone has the right food type
                if (currentFoodType != null && currentFoodType == enclosure.getDiet()) {
                    fedEnclosures.add(enclosure);
                    
                    // Mark the enclosure as fed
                    enclosure.setFed(true);
                }
            }
        }
        
        return fedEnclosures;
    }
    
    /**
     * Level-specific optimization method.
     * Each level needs to implement this method to handle its specific constraints.
     * 
     * @param depot The depot location
     * @param enclosures List of animal enclosures
     * @param foodStorages List of food storages
     * @return List of optimized paths that respect battery constraints
     */
    protected abstract List<List<Location>> optimizeForLevel(
            Depot depot,
            List<AnimalEnclosure> enclosures,
            List<FoodStorage> foodStorages);
}