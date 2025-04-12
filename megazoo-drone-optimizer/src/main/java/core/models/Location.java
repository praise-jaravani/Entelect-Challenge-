/**
 * Represents a physical point in the 3D zoo space.
 * I chose to make this immutable because locations shouldn't change after creation -
 * it prevents accidental modifications during path calculations.
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class Location {
    // Using final fields because coordinates are fundamental properties
    // that shouldn't change during operations
    private final double x;
    private final double y;
    private final double z;

    /**
     * I'm including z-axis even though drones fly at fixed height because:
     * 1. The depot/food locations might have different elevations
     * 2. It future-proofs for potential 3D path requirements
     */
    public Location(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * I prefer exposing coordinates through getters rather than direct field access
     * to maintain control over how location data is used
     */
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    /**
     * I'll need this for distance calculations between locations.
     * Made it static because it's a utility function that doesn't depend on instance state.
     */
    public static double calculateDistance(Location a, Location b) {
        // Using standard Euclidean distance because:
        // 1. Zoo space is Cartesian according to specs
        // 2. No mention of non-linear distance metrics
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * I'm overriding equals/hashCode because:
     * 1. Need to compare locations for path planning
     * 2. Want to use Locations as keys in spatial maps later
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location location = (Location) o;
        // Using Double.compare for safe floating-point comparison
        return Double.compare(location.x, x) == 0 &&
                Double.compare(location.y, y) == 0 &&
                Double.compare(location.z, z) == 0;
    }

    @Override
    public int hashCode() {
        // Using Objects.hash for consistent hashing
        return Objects.hash(x, y, z);
    }

    /**
     * Added toString for debugging - I'll probably need to visualize paths
     */
    @Override
    public String toString() {
        // Formatting as (x,y,z) to match input file convention
        return String.format("(%.1f,%.1f,%.1f)", x, y, z);
    }
}