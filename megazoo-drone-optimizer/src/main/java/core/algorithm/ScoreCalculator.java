package main.java.core.algorithm;

import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.Location;

import java.util.List;

/**
 * Calculates the score for drone runs based on the animals fed and distance traveled.
 * Score formula: sum(importance * 1000) - totalDistance
 */
public class ScoreCalculator {
    
    private final MockDistanceCalculator distanceCalculator;
    
    /**
     * Creates a ScoreCalculator with a specific distance calculator.
     * 
     * @param distanceCalculator the calculator to use for distance measurements
     */
    public ScoreCalculator(MockDistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }
    
    /**
     * Creates a ScoreCalculator with a default mock distance calculator.
     * 
     * @param maxFlightHeight the maximum height for drone flight
     */
    public ScoreCalculator(int maxFlightHeight) {
        this.distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
    }
    
    /**
     * Calculate score for a single drone run.
     * 
     * @param path Complete path taken by drone (must start and end at depot)
     * @param fedEnclosures Enclosures that were successfully fed
     * @return Score according to formula: sum(importance * 1000) - totalDistance
     * @throws IllegalArgumentException if path is empty or doesn't start/end at the same location
     */
    public double calculateScore(List<Location> path, List<AnimalEnclosure> fedEnclosures) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }
        
        // Verify path starts and ends at the same location (depot)
        if (!path.get(0).equals(path.get(path.size() - 1))) {
            throw new IllegalArgumentException("Path must start and end at the same location (depot)");
        }
        
        // Calculate total importance from fed enclosures
        double totalImportance = 0.0;
        for (AnimalEnclosure enclosure : fedEnclosures) {
            totalImportance += enclosure.getImportance();
        }
        
        // Calculate total distance traveled
        double totalDistance = distanceCalculator.calculatePathDistance(path);
        
        // Apply score formula: sum(importance * 1000) - totalDistance
        return (totalImportance * 1000) - totalDistance;
    }
    
    /**
     * Calculate scores for multiple drone runs (battery swaps) and sum them.
     * 
     * @param paths List of paths for each battery charge (each starts/ends at depot)
     * @param fedEnclosuresList List of enclosures fed in each path
     * @return Total score across all paths
     */
    public double calculateTotalScore(List<List<Location>> paths, List<List<AnimalEnclosure>> fedEnclosuresList) {
        if (paths == null || fedEnclosuresList == null) {
            throw new IllegalArgumentException("Paths and fedEnclosuresList cannot be null");
        }
        
        if (paths.size() != fedEnclosuresList.size()) {
            throw new IllegalArgumentException("Number of paths must match number of fed enclosures lists");
        }
        
        double totalScore = 0.0;
        
        for (int i = 0; i < paths.size(); i++) {
            totalScore += calculateScore(paths.get(i), fedEnclosuresList.get(i));
        }
        
        return totalScore;
    }
    
    /**
     * Estimate if a path would result in a positive score.
     * Useful for determining if a potential path is worth taking.
     * 
     * @param path Proposed path
     * @param fedEnclosures Enclosures that would be fed
     * @return true if the estimated score is positive, false otherwise
     */
    public boolean isPathWorthTaking(List<Location> path, List<AnimalEnclosure> fedEnclosures) {
        // Quick estimation for planning purposes
        double score = calculateScore(path, fedEnclosures);
        return score > 0;
    }
    
    /**
     * Compare two potential paths and determine which has a better score.
     * 
     * @param path1 First path to compare
     * @param fedEnclosures1 Enclosures fed in first path
     * @param path2 Second path to compare
     * @param fedEnclosures2 Enclosures fed in second path
     * @return positive value if path1 is better, negative if path2 is better, 0 if equal
     */
    public double comparePathScores(
            List<Location> path1, List<AnimalEnclosure> fedEnclosures1,
            List<Location> path2, List<AnimalEnclosure> fedEnclosures2) {
        
        double score1 = calculateScore(path1, fedEnclosures1);
        double score2 = calculateScore(path2, fedEnclosures2);
        
        return score1 - score2;
    }
}