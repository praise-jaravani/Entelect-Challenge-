package main.java.core.mock;

import core.models.FoodStorage;

/**
 * Mock implementation of the FoodStorage interface for testing purposes.
 * Represents a food storage location in the zoo where drones can pick up food.
 */
public class MockFoodStorage extends MockLocation implements FoodStorage {
    // Represents the food type: 'c' for carnivore, 'h' for herbivore, 'o' for omnivore
    private final char foodType;
    
    /**
     * Create a new mock food storage with the specified coordinates and food type.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     * @param foodType the type of food stored ('c', 'h', or 'o')
     */
    public MockFoodStorage(int x, int y, int z, char foodType) {
        super(x, y, z);
        if (foodType != 'c' && foodType != 'h' && foodType != 'o') {
            throw new IllegalArgumentException("Food type must be 'c', 'h', or 'o'");
        }
        this.foodType = foodType;
    }
    
    /**
     * @return the food type in this storage
     */
    @Override
    public char getFoodType() {
        return foodType;
    }
    
    @Override
    public String toString() {
        return "FoodStorage(" + getX() + "," + getY() + "," + getZ() + "," + foodType + ")";
    }
}