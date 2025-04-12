package main.java.core.mock;

import core.models.AnimalEnclosure;

/**
 * Mock implementation of the AnimalEnclosure interface for testing purposes.
 * Represents an animal enclosure in the zoo that needs to be fed.
 */
public class MockAnimalEnclosure extends MockLocation implements AnimalEnclosure {
    // Represents the food type: 'c' for carnivore, 'h' for herbivore, 'o' for omnivore
    private final char diet;
    // Importance multiplier for scoring
    private final double importance;
    // Track if this enclosure has been fed
    private boolean fed = false;
    
    /**
     * Create a new mock animal enclosure with the specified parameters.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     * @param importance the importance multiplier for this enclosure
     * @param diet the type of diet required ('c', 'h', or 'o')
     */
    public MockAnimalEnclosure(int x, int y, int z, double importance, char diet) {
        super(x, y, z);
        if (diet != 'c' && diet != 'h' && diet != 'o') {
            throw new IllegalArgumentException("Diet must be 'c', 'h', or 'o'");
        }
        this.importance = importance;
        this.diet = diet;
    }
    
    /**
     * @return the diet required by this enclosure
     */
    @Override
    public char getDiet() {
        return diet;
    }
    
    /**
     * @return the importance multiplier for this enclosure
     */
    @Override
    public double getImportance() {
        return importance;
    }
    
    /**
     * @return true if this enclosure has been fed, false otherwise
     */
    @Override
    public boolean isFed() {
        return fed;
    }
    
    /**
     * Marks this enclosure as fed.
     */
    @Override
    public void setFed(boolean fed) {
        this.fed = fed;
    }
    
    @Override
    public String toString() {
        return "AnimalEnclosure(" + getX() + "," + getY() + "," + getZ() + 
               "," + importance + "," + diet + ",fed=" + fed + ")";
    }
}