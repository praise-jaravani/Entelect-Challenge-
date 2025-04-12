package main.java.core.mock;

import core.models.Location;

/**
 * Mock implementation of the Location interface for testing purposes.
 * This class provides a simple implementation of a location with x, y, and z coordinates.
 */
public class MockLocation implements Location {
    private final int x, y, z;
    
    /**
     * Create a new mock location with the specified coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
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
        return "Location(" + x + "," + y + "," + z + ")";
    }
}