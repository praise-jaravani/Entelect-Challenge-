/**
 * Represents the drone's home base in the zoo.
 * I'm modeling this as a specialized Location because:
 * 1. It shares core coordinate functionality with other locations
 * 2. It needs additional drone-specific behaviors
 * 3. The spec confirms all drone paths must start/end here
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class Depot extends Location {
    /**
     * The standard flight altitude (50m) is hardcoded because:
     * 1. The spec consistently mentions this fixed value
     * 2. All takeoff/landing sequences depend on it
     */
    private static final int FLIGHT_ALTITUDE = 50;

    /**
     * I'm keeping the constructor simple since depots don't have
     * special properties beyond coordinates in the input files
     */
    public Depot(double x, double y, double z) {
        super(x, y, z);
    }

    /**
     * Calculates vertical takeoff distance to flight altitude.
     * Made this a method because:
     * 1. Battery calculations need this frequently
     * 2. The 50m rule appears in multiple spec sections
     */
    public double getTakeoffDistance() {
        // Using absolute value because spec doesn't prohibit underground depots
        return Math.abs(FLIGHT_ALTITUDE - getZ());
    }

    /**
     * Mirror of takeoff for landing calculations.
     * Separate method for clarity even though math is identical
     */
    public double getLandingDistance() {
        return getTakeoffDistance();
    }

    /**
     * Overriding toString to match submission format requirements.
     * Omitting z-coordinate as specified in "Submission" section
     */
    @Override
    public String toString() {
        return String.format("(%.0f,%.0f)", getX(), getY());
    }
}