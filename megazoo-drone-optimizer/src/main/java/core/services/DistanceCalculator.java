/**
 * Calculates 3D distances with special handling for drone flight mechanics.
 * All distances are in meters as specified in the problem constraints.
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class DistanceCalculator {

    /**
     * Calculates full movement distance between two points including:
     * 1. Vertical takeoff/landing at 50m altitude
     * 2. Horizontal Euclidean distance
     *
     * @param from Starting location (depot, storage, or enclosure)
     * @param to Target location
     * @return Total movement distance in meters
     */
    public static double calculateMovementDistance(Location from, Location to) {
        // Vertical components (spec requires reaching 50m flight altitude)
        double verticalDistance = Math.abs(50 - from.getZ()) + Math.abs(50 - to.getZ());

        // Horizontal component (standard Euclidean distance)
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double horizontalDistance = Math.sqrt(dx*dx + dy*dy);

        return verticalDistance + horizontalDistance;
    }

    /**
     * Special case for depot-to-depot paths (battery swaps)
     * Only includes vertical movement since drone doesn't move horizontally
     */
    public static double calculateBatterySwapDistance(Depot depot) {
        return 2 * Math.abs(50 - depot.getZ()); // Up and down
    }

    /**
     * Checks if two locations are at the same horizontal position
     * (Used for food storage/enclosure landing decisions)
     */
    public static boolean isSameHorizontalPosition(Location a, Location b) {
        return a.getX() == b.getX() && a.getY() == b.getY();
    }
}