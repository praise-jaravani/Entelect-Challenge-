/**
 * Represents an animal enclosure that needs feeding.
 * Tracks location, diet requirements, and feeding priority.
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class AnimalEnclosure extends Location {
    private final double importance;
    private final char dietType;  // 'c', 'h', or 'o'
    private boolean isFed;

    public AnimalEnclosure(double x, double y, double z, double importance, char dietType) {
        super(x, y, z);
        this.importance = importance;
        this.dietType = dietType;
        this.isFed = false;
    }

    public double getImportance() {
        return importance;
    }

    public char getDietType() {
        return dietType;
    }

    public boolean isFed() {
        return isFed;
    }

    public void markAsFed() {
        isFed = true;
    }

    @Override
    public String toString() {
        return String.format("(%.0f,%.0f)", getX(), getY());
    }
}