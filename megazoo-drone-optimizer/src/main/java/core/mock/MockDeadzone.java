package main.java.core.mock;

import core.models.Deadzone;

/**
 * Mock implementation of the Deadzone interface for testing purposes.
 * Represents a circular deadzone in the zoo that drones must avoid.
 */
public class MockDeadzone implements Deadzone {
    private final int x, y;
    private final int radius;
    
    /**
     * Create a new mock deadzone with the specified parameters.
     *
     * @param x x-coordinate of the center
     * @param y y-coordinate of the center
     * @param radius radius of the deadzone
     */
    public MockDeadzone(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }
    
    /**
     * @return x-coordinate of the center
     */
    @Override
    public int getX() {
        return x;
    }
    
    /**
     * @return y-coordinate of the center
     */
    @Override
    public int getY() {
        return y;
    }
    
    /**
     * @return the radius of the deadzone
     */
    @Override
    public int getRadius() {
        return radius;
    }
    
    /**
     * Check if a line from (x1,y1) to (x2,y2) intersects with this deadzone.
     * 
     * @param x1 x-coordinate of first point
     * @param y1 y-coordinate of first point
     * @param x2 x-coordinate of second point
     * @param y2 y-coordinate of second point
     * @return true if the line intersects the deadzone, false otherwise
     */
    @Override
    public boolean intersectsLine(int x1, int y1, int x2, int y2) {
        // Calculate the closest point on the line segment to the center of the circle
        double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        
        // Line vector components
        double dx = (x2 - x1) / length;
        double dy = (y2 - y1) / length;
        
        // Vector from line start to circle center
        double t = dx * (x - x1) + dy * (y - y1);
        
        // Clamp t to line segment bounds
        t = Math.max(0, Math.min(length, t));
        
        // Closest point coordinates
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        
        // Distance from closest point to circle center
        double distance = Math.sqrt(Math.pow(closestX - x, 2) + Math.pow(closestY - y, 2));
        
        // Intersection occurs if distance is less than or equal to radius
        return distance <= radius;
    }
    
    @Override
    public String toString() {
        return "Deadzone(" + x + "," + y + ",r=" + radius + ")";
    }
}