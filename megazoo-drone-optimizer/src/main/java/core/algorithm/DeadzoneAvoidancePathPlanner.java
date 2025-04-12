package main.java.core.algorithm;

import main.java.core.mock.MockDistanceCalculator;
import main.java.core.models.AnimalEnclosure;
import main.java.core.models.Deadzone;
import main.java.core.models.FoodStorage;
import main.java.core.models.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An extension of the GreedyPathPlanner that avoids deadzones.
 * This planner checks if paths intersect with deadzones and finds alternative routes when needed.
 */
public class DeadzoneAvoidancePathPlanner extends GreedyPathPlanner {
    
    private final List<Deadzone> deadzones;
    private final MockDistanceCalculator distanceCalculator;
    
    /**
     * Creates a DeadzoneAvoidancePathPlanner with a specific distance calculator and deadzones.
     * 
     * @param distanceCalculator the calculator to use for distance measurements
     * @param deadzones the list of deadzones to avoid
     */
    public DeadzoneAvoidancePathPlanner(MockDistanceCalculator distanceCalculator, List<Deadzone> deadzones) {
        super(distanceCalculator);
        this.distanceCalculator = distanceCalculator;
        this.deadzones = new ArrayList<>(deadzones);
    }
    
    /**
     * Creates a DeadzoneAvoidancePathPlanner with a default mock distance calculator.
     * 
     * @param maxFlightHeight the maximum height for drone flight
     * @param deadzones the list of deadzones to avoid
     */
    public DeadzoneAvoidancePathPlanner(int maxFlightHeight, List<Deadzone> deadzones) {
        super(maxFlightHeight);
        this.distanceCalculator = new MockDistanceCalculator(maxFlightHeight);
        this.deadzones = new ArrayList<>(deadzones);
    }
    
    /**
     * Plan an optimal path to feed animals within battery constraints, avoiding deadzones.
     * 
     * @param depot Starting and ending location
     * @param enclosuresList List of animal enclosures that need feeding
     * @param foodStoragesList List of food storage locations
     * @param batteryCapacity Maximum distance allowed for this path
     * @return List of locations to visit in order, starting and ending with depot
     */
    @Override
    public List<Location> planPath(
            Location depot,
            List<? extends Location> enclosuresList,
            List<? extends Location> foodStoragesList,
            double batteryCapacity) {
        
        // First, generate a path using the base greedy algorithm
        List<Location> basePath = super.planPath(depot, enclosuresList, foodStoragesList, batteryCapacity);
        
        // If there are no deadzones or the path is empty, return the base path
        if (deadzones.isEmpty() || basePath.size() <= 1) {
            return basePath;
        }
        
        // Check if the base path intersects with any deadzones
        List<List<Location>> pathSegments = splitIntoSegments(basePath);
        boolean hasIntersection = false;
        
        for (List<Location> segment : pathSegments) {
            if (segment.size() < 2) continue;
            
            Location start = segment.get(0);
            Location end = segment.get(1);
            
            if (intersectsDeadzone(start, end)) {
                hasIntersection = true;
                break;
            }
        }
        
        // If no intersections, return the original path
        if (!hasIntersection) {
            return basePath;
        }
        
        // Otherwise, create a safe path by routing around deadzones
        return createSafePath(basePath, batteryCapacity);
    }
    
    /**
     * Check if a path segment from start to end intersects with any deadzone.
     * 
     * @param start Starting location
     * @param end Ending location
     * @return true if the path intersects with a deadzone, false otherwise
     */
    private boolean intersectsDeadzone(Location start, Location end) {
        for (Deadzone deadzone : deadzones) {
            if (deadzone.intersectsLine(start.getX(), start.getY(), end.getX(), end.getY())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Split a path into segments for deadzone checking.
     * 
     * @param path The full path
     * @return List of path segments (pairs of consecutive locations)
     */
    private List<List<Location>> splitIntoSegments(List<Location> path) {
        List<List<Location>> segments = new ArrayList<>();
        
        if (path.size() < 2) {
            return segments;
        }
        
        for (int i = 0; i < path.size() - 1; i++) {
            List<Location> segment = new ArrayList<>();
            segment.add(path.get(i));
            segment.add(path.get(i + 1));
            segments.add(segment);
        }
        
        return segments;
    }
    
    /**
     * Create a safe path that avoids deadzones.
     * Uses waypoints to route around deadzones.
     * 
     * @param originalPath The original path that may intersect deadzones
     * @param batteryCapacity Maximum battery capacity
     * @return A new path that avoids deadzones
     */
    private List<Location> createSafePath(List<Location> originalPath, double batteryCapacity) {
        List<Location> safePath = new ArrayList<>();
        safePath.add(originalPath.get(0)); // Start with depot
        
        Location currentLocation = originalPath.get(0);
        double remainingBattery = batteryCapacity;
        
        // Track current food type
        Character currentFoodType = null;
        
        // Process each location in the original path
        for (int i = 1; i < originalPath.size(); i++) {
            Location nextLocation = originalPath.get(i);
            
            // If path doesn't intersect deadzone, add directly
            if (!intersectsDeadzone(currentLocation, nextLocation)) {
                double distance = distanceCalculator.calculateDroneDistance(currentLocation, nextLocation);
                
                // Check if we have enough battery
                if (distance > remainingBattery) {
                    break; // Not enough battery, end path
                }
                
                // Update food type if visiting food storage
                if (nextLocation instanceof FoodStorage) {
                    currentFoodType = ((FoodStorage) nextLocation).getFoodType();
                }
                
                // Add location to path
                safePath.add(nextLocation);
                remainingBattery -= distance;
                currentLocation = nextLocation;
            } else {
                // Path intersects deadzone - find waypoints to avoid it
                List<Location> waypoints = findWaypointsAroundDeadzones(currentLocation, nextLocation);
                
                // Check if we can reach all waypoints and the next location
                double totalWaypointDistance = 0;
                Location waypointStart = currentLocation;
                
                boolean canReachThroughWaypoints = true;
                for (Location waypoint : waypoints) {
                    double waypointDistance = distanceCalculator.calculateDroneDistance(waypointStart, waypoint);
                    totalWaypointDistance += waypointDistance;
                    
                    if (totalWaypointDistance > remainingBattery) {
                        canReachThroughWaypoints = false;
                        break;
                    }
                    
                    waypointStart = waypoint;
                }
                
                // Final leg from last waypoint to next location
                if (canReachThroughWaypoints) {
                    double finalLegDistance = distanceCalculator.calculateDroneDistance(
                            waypointStart, nextLocation);
                    
                    if (totalWaypointDistance + finalLegDistance > remainingBattery) {
                        canReachThroughWaypoints = false;
                    }
                }
                
                if (canReachThroughWaypoints) {
                    // Add all waypoints to path
                    for (Location waypoint : waypoints) {
                        double distance = distanceCalculator.calculateDroneDistance(currentLocation, waypoint);
                        safePath.add(waypoint);
                        remainingBattery -= distance;
                        currentLocation = waypoint;
                    }
                    
                    // Then add the next location
                    double distance = distanceCalculator.calculateDroneDistance(currentLocation, nextLocation);
                    
                    // Update food type if visiting food storage
                    if (nextLocation instanceof FoodStorage) {
                        currentFoodType = ((FoodStorage) nextLocation).getFoodType();
                    }
                    
                    safePath.add(nextLocation);
                    remainingBattery -= distance;
                    currentLocation = nextLocation;
                } else {
                    // Skip this location if we can't reach it safely
                    continue;
                }
            }
        }
        
        // Ensure path ends at depot
        if (safePath.size() > 1 && !safePath.get(safePath.size() - 1).equals(originalPath.get(0))) {
            Location depot = originalPath.get(0);
            double distanceToDepot = distanceCalculator.calculateDroneDistance(
                    safePath.get(safePath.size() - 1), depot);
            
            if (distanceToDepot <= remainingBattery) {
                safePath.add(depot);
            } else {
                // Not enough battery to return to depot - path is invalid
                // Return just the depot to indicate an invalid path
                return new ArrayList<>(List.of(originalPath.get(0)));
            }
        }
        
        return safePath;
    }
    
    /**
     * Find waypoints to navigate around deadzones between two locations.
     * 
     * @param start Starting location
     * @param end Ending location
     * @return List of waypoints to navigate safely
     */
    private List<Location> findWaypointsAroundDeadzones(Location start, Location end) {
        List<Location> waypoints = new ArrayList<>();
        
        // Find deadzones that intersect the path
        List<Deadzone> intersectingDeadzones = new ArrayList<>();
        for (Deadzone deadzone : deadzones) {
            if (deadzone.intersectsLine(start.getX(), start.getY(), end.getX(), end.getY())) {
                intersectingDeadzones.add(deadzone);
            }
        }
        
        if (intersectingDeadzones.isEmpty()) {
            return waypoints; // No intersections, return empty list
        }
        
        // For each intersecting deadzone, create waypoints to navigate around it
        for (Deadzone deadzone : intersectingDeadzones) {
            // Find directions to avoid the deadzone
            int x = deadzone.getX();
            int y = deadzone.getY();
            int r = deadzone.getRadius();
            
            // Calculate angle from deadzone center to end point
            double angleToEnd = Math.atan2(end.getY() - y, end.getX() - x);
            
            // Create two waypoints to navigate around the deadzone
            // Use perpendicular directions to create a detour
            double detourAngle1 = angleToEnd + Math.PI / 2;  // 90 degrees clockwise
            double detourAngle2 = angleToEnd - Math.PI / 2;  // 90 degrees counterclockwise
            
            // Calculate waypoint positions (add buffer of 5m to radius)
            int buffer = 5;
            int waypointDistance = r + buffer;
            
            int wx1 = x + (int)(waypointDistance * Math.cos(detourAngle1));
            int wy1 = y + (int)(waypointDistance * Math.sin(detourAngle1));
            int wx2 = x + (int)(waypointDistance * Math.cos(detourAngle2));
            int wy2 = y + (int)(waypointDistance * Math.sin(detourAngle2));
            
            // Create waypoint locations (use max flight height for z)
            MockLocation waypoint1 = new MockLocation(wx1, wy1, 50);
            MockLocation waypoint2 = new MockLocation(wx2, wy2, 50);
            
            // Check which waypoint is closer to the end point
            double dist1 = Math.hypot(end.getX() - wx1, end.getY() - wy1);
            double dist2 = Math.hypot(end.getX() - wx2, end.getY() - wy2);
            
            // Add the closer waypoint to our list
            if (dist1 <= dist2) {
                waypoints.add(waypoint1);
            } else {
                waypoints.add(waypoint2);
            }
        }
        
        return waypoints;
    }
    
    /**
     * Mock Location class for creating waypoints.
     */
    private static class MockLocation implements Location {
        private final int x, y, z;
        
        public MockLocation(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public int getX() { return x; }
        
        @Override
        public int getY() { return y; }
        
        @Override
        public int getZ() { return z; }
        
        @Override
        public String toString() {
            return "Waypoint(" + x + "," + y + "," + z + ")";
        }
    }
}