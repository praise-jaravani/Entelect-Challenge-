/**
 * Represents a food storage location in the zoo.
 * Stores one specific diet type that drones can collect.
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class FoodStorage extends Location {
    private final char dietType;  // 'c', 'h', or 'o' per spec

    public FoodStorage(double x, double y, double z, char dietType) {
        super(x, y, z);
        this.dietType = dietType;
    }

    public char getDietType() {
        return dietType;
    }

    @Override
    public String toString() {
        return String.format("(%.0f,%.0f)", getX(), getY());
    }
}