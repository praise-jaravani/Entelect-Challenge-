package main.java.core.mock;

import core.models.Depot;

/**
 * Mock implementation of the Depot interface for testing purposes.
 * Represents the drone depot where drones start and end their journeys.
 */
public class MockDepot extends MockLocation implements Depot {
    
    /**
     * Create a new mock depot with the specified coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
    public MockDepot(int x, int y, int z) {
        super(x, y, z);
    }
    
    @Override
    public String toString() {
        return "Depot(" + getX() + "," + getY() + "," + getZ() + ")";
    }
}