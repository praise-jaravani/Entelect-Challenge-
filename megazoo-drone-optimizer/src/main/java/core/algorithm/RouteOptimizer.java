package main.java.core.algorithm;

import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.FoodStorage;
import main.java.core.models.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimizes routes for drone feeding paths by splitting them into battery-compliant segments.
 * Handles ensuring that paths respect battery limitations while maximizing feeding efficiency.
 */
public class RouteOptimizer {
    
    private final MockDistanceCalculator distanceCalculator;
    
    /**
     * Creates a RouteOptimizer with a specific distance calculator.
     * 
     * @param distanceCalculator the calculator to use for distance measurements
     */
    public RouteOptimizer(MockDistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }
    
    /**
     * Creates a RouteOptimizer with a default mock distance calculator.
     * 
     * @param maxFlightHeight the maximum height for drone flight
     */
    public RouteOptimizer(int maxFlightHeight) {
        this.distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
    }
    
    /**
     * Split a proposed path into multiple battery-compliant segments.
     * Each segment starts and ends at the depot.
     * 
     * @param proposedPath The ideal path if battery were unlimited
     * @param depot The depot location for swaps
     * @param batteryCapacity Maximum distance per charge
     * @return List of battery-compliant paths (each starting/ending at depot)
     */
    public List<List<Location>> optimizeForBatterySwaps(
            List<Location> proposedPath,
            Location depot,
            double batteryCapacity) {
        
        List<List<Location>> result = new ArrayList<>();
        
        // If proposed path is empty or has only the depot, return empty list
        if (proposedPath == null || proposedPath.size() <= 1) {
            return result;
        }
        
        // If proposed path doesn't start or end with depot, return empty list
        if (!proposedPath.get(0).equals(depot) || !proposedPath.get(proposedPath.size() - 1).equals(depot)) {
            throw new IllegalArgumentException("Proposed path must start and end with depot");
        }
        
        // Remove consecutive duplicate locations if any
        List<Location> cleanPath = removeDuplicateConsecutiveLocations(proposedPath);
        
        // Keep track of which enclosures have been fed (to avoid feeding again)
        Map<Location, Boolean> fedEnclosures = new HashMap<>();
        
        // Start building segments
        int currentIndex = 1; // Skip the first depot
        while (currentIndex < cleanPath.size() - 1) { // Skip the last depot
            
            // Start a new path segment from the depot
            List<Location> segment = new ArrayList<>();
            segment.add(depot);
            
            // Current location is the depot
            Location currentLocation = depot;
            
            // Track remaining battery for this segment
            double remainingBattery = batteryCapacity;
            
            // Track current food type
            Character currentFoodType = null;
            
            // Keep adding locations until we run out of battery or finish the path
            while (currentIndex < cleanPath.size() - 1) {
                Location nextLocation = cleanPath.get(currentIndex);
                
                // Calculate distance to next location
                double distanceToNext = distanceCalculator.calculateDroneDistance(currentLocation, nextLocation);
                
                // Calculate distance from next location back to depot
                double distanceToDepot = distanceCalculator.calculateDroneDistance(nextLocation, depot);
                
                // Check if we have enough battery to visit next location and return to depot
                if (distanceToNext + distanceToDepot > remainingBattery) {
                    // Not enough battery, need to return to depot
                    break;
                }
                
                // Skip this location if it's an enclosure that's already been fed
                if (nextLocation instanceof AnimalEnclosure && fedEnclosures.getOrDefault(nextLocation, false)) {
                    currentIndex++;
                    continue;
                }
                
                // If it's a food storage, update current food type
                if (nextLocation instanceof FoodStorage) {
                    currentFoodType = ((FoodStorage) nextLocation).getFoodType();
                }
                
                // If it's an enclosure, check if we have the right food type
                if (nextLocation instanceof AnimalEnclosure) {
                    AnimalEnclosure enclosure = (AnimalEnclosure) nextLocation;
                    
                    // If we don't have the right food type, skip this enclosure
                    if (currentFoodType == null || currentFoodType != enclosure.getDiet()) {
                        currentIndex++;
                        continue;
                    }
                    
                    // Mark this enclosure as fed
                    fedEnclosures.put(nextLocation, true);
                }
                
                // Add this location to the segment
                segment.add(nextLocation);
                
                // Update battery and current location
                remainingBattery -= distanceToNext;
                currentLocation = nextLocation;
                
                // Move to next location in proposed path
                currentIndex++;
            }
            
            // Add return to depot if not already there
            if (!currentLocation.equals(depot)) {
                segment.add(depot);
            }
            
            // Add this segment to the result if it visits at least one location besides depot
            if (segment.size() > 2) {
                result.add(segment);
            } else {
                // If we couldn't visit any locations, we need to break the loop
                // to avoid an infinite loop
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Optimize a path by finding the most efficient order to visit food storages and enclosures.
     * This is useful for Level 2+ where multiple battery swaps are available.
     * 
     * @param depot Starting and ending location
     * @param enclosures List of enclosures to feed
     * @param foodStorages List of food storages available
     * @param batteryCapacity Maximum distance per charge
     * @param maxBatterySwaps Maximum number of battery swaps allowed
     * @return List of optimized paths that respect battery constraints
     */
    public List<List<Location>> optimizeMultipleRoutes(
            Location depot,
            List<AnimalEnclosure> enclosures,
            List<FoodStorage> foodStorages,
            double batteryCapacity,
            int maxBatterySwaps) {
        
        // Create a copy of enclosures to track which ones have been fed
        List<AnimalEnclosure> remainingEnclosures = new ArrayList<>(enclosures);
        
        // Result list to hold all paths
        List<List<Location>> result = new ArrayList<>();
        
        // Use greedy path planner to generate paths
        GreedyPathPlanner pathPlanner = new GreedyPathPlanner(distanceCalculator);
        
        // Keep generating paths until we've used all battery swaps or fed all enclosures
        for (int i = 0; i <= maxBatterySwaps && !remainingEnclosures.isEmpty(); i++) {
            // Generate a path for the current battery
            List<Location> path = pathPlanner.planPath(depot, remainingEnclosures, foodStorages, batteryCapacity);
            
            // If the path only contains the depot, break (no more useful paths)
            if (path.size() <= 2) {
                break;
            }
            
            // Add path to result
            result.add(path);
            
            // Update remaining enclosures list
            updateRemainingEnclosures(path, remainingEnclosures);
        }
        
        return result;
    }
    
    /**
     * Updates the list of remaining enclosures by removing those that were fed in the path.
     * 
     * @param path The path that was just completed
     * @param remainingEnclosures The list of enclosures that still need to be fed
     */
    private void updateRemainingEnclosures(List<Location> path, List<AnimalEnclosure> remainingEnclosures) {
        for (Location location : path) {
            if (location instanceof AnimalEnclosure) {
                AnimalEnclosure enclosure = (AnimalEnclosure) location;
                remainingEnclosures.remove(enclosure);
            }
        }
    }
    
    /**
     * Remove consecutive duplicate locations from a path.
     * This is needed because sometimes the proposed path might have repetitions.
     * 
     * @param path The original path
     * @return A path with no consecutive duplicates
     */
    private List<Location> removeDuplicateConsecutiveLocations(List<Location> path) {
        if (path == null || path.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Location> result = new ArrayList<>();
        Location previous = null;
        
        for (Location current : path) {
            if (previous == null || !current.equals(previous)) {
                result.add(current);
            }
            previous = current;
        }
        
        return result;
    }
    
    /**
     * Estimate if a multi-segment path is feasible with the given battery constraints.
     * 
     * @param paths List of path segments
     * @param batteryCapacity Maximum distance per charge
     * @return true if all segments are within battery capacity, false otherwise
     */
    public boolean arePathsFeasible(List<List<Location>> paths, double batteryCapacity) {
        if (paths == null) {
            return false;
        }
        
        for (List<Location> path : paths) {
            double pathDistance = distanceCalculator.calculatePathDistance(path);
            if (pathDistance > batteryCapacity) {
                return false;
            }
        }
        
        return true;
    }
}