/**
 * Tracks drone state during path execution including:
 * - Current position and carried food type
 * - Battery consumption
 * - Feeding completion status
 *
 * @author Karabo Motsileng
 * @version 12 April
 */
public class Drone {
    private Location currentPosition;
    private char currentFoodType; // 'c', 'h', 'o', or '\0' if empty
    private double remainingBattery;
    private final double maxBattery;

    public Drone(Depot depot, double maxBattery) {
        this.currentPosition = depot;
        this.currentFoodType = '\0';
        this.remainingBattery = maxBattery;
        this.maxBattery = maxBattery;
    }

    /**
     * Attempts movement to target location, returns success status.
     * Automatically deducts battery and updates position.
     */
    public boolean moveTo(Location target) {
        double distance = DistanceCalculator.calculateMovementDistance(currentPosition, target);
        if (distance > remainingBattery) {
            return false; // Would cause crash
        }
        remainingBattery -= distance;
        currentPosition = target;
        return true;
    }

    /**
     * Handles food pickup logic at storage locations.
     * Returns whether food type changed.
     */
    public boolean pickupFood(char newFoodType) {
        if (currentFoodType != newFoodType) {
            currentFoodType = newFoodType;
            return true;
        }
        return false;
    }

    /**
     * Resets battery and position for new path after depot return.
     */
    public void swapBattery(Depot depot) {
        this.currentPosition = depot;
        this.remainingBattery = maxBattery;
        // Keeps current food type (spec doesn't specify clearing)
    }

    // Accessors
    public Location getCurrentPosition() { return currentPosition; }
    public char getCurrentFoodType() { return currentFoodType; }
    public double getRemainingBattery() { return remainingBattery; }
}