package main.java.core.mock;

import core.models.Location;
import core.services.DistanceCalculator;

/**
 * Mock implementation of distance calculations for drone movement.
 * Handles the special movement rules for the drone including vertical takeoff/landing
 * and flight at maximum height.
 */
public class MockDistanceCalculator implements DistanceCalculator {
    
    private final int maxFlightHeight;
    
    /**
     * Create a new MockDistanceCalculator with the specified maximum flight height.
     *
     * @param maxFlightHeight the maximum height at which drones fly
     */
    public MockDistanceCalculator(int maxFlightHeight) {
        this.maxFlightHeight = maxFlightHeight;
    }
    
    /**
     * Calculate simple Euclidean distance between two locations.
     * This doesn't account for drone movement rules and is used for general distance calculations.
     *
     * @param a first location
     * @param b second location
     * @return the straight-line distance between the locations
     */
    public static double calculateDistance(Location a, Location b) {
        return Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2) +
            Math.pow(a.getY() - b.getY(), 2) +
            Math.pow(a.getZ() - b.getZ(), 2)
        );
    }
    
    /**
     * Calculate the distance a drone would travel between two locations following drone movement rules:
     * 1. Drone must first ascend/descend vertically to flight height
     * 2. Drone moves horizontally at flight height
     * 3. Drone must ascend/descend vertically to destination
     *
     * @param a starting location
     * @param b ending location
     * @return the actual distance the drone would travel
     */
    public double calculateDroneDistance(Location a, Location b) {
        double distance = 0;
        
        // Step 1: Vertical takeoff from starting point to flight height
        if (a.getZ() != maxFlightHeight) {
            distance += Math.abs(maxFlightHeight - a.getZ());
        }
        
        // Step 2: Horizontal movement at flight height
        distance += Math.sqrt(
            Math.pow(b.getX() - a.getX(), 2) +
            Math.pow(b.getY() - a.getY(), 2)
        );
        
        // Step 3: Vertical landing from flight height to destination
        if (b.getZ() != maxFlightHeight) {
            distance += Math.abs(maxFlightHeight - b.getZ());
        }
        
        return distance;
    }
    
    /**
     * Calculate the total distance of a drone path through a series of locations.
     *
     * @param path list of locations in order of visitation
     * @return the total distance the drone would travel along the path
     */
    public double calculatePathDistance(Iterable<Location> path) {
        double totalDistance = 0;
        Location previous = null;
        
        for (Location current : path) {
            if (previous != null) {
                totalDistance += calculateDroneDistance(previous, current);
            }
            previous = current;
        }
        
        return totalDistance;
    }
    
    /**
     * Check if a path segment intersects with any deadzones.
     *
     * @param a starting location
     * @param b ending location
     * @param deadzones list of deadzones to check against
     * @return true if the path intersects with any deadzone, false otherwise
     */
    public boolean intersectsDeadzone(Location a, Location b, Iterable<MockDeadzone> deadzones) {
        for (MockDeadzone deadzone : deadzones) {
            if (deadzone.intersectsLine(a.getX(), a.getY(), b.getX(), b.getY())) {
                return true;
            }
        }
        return false;
    }
}