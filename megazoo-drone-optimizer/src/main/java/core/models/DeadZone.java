/**
 * Represents a no-fly zone where drones will crash.
 * Modeled as an infinite cylinder with:
 * - Circular base (x,y center + radius)
 * - Infinite height (z-axis)
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class DeadZone {
    private final double centerX;
    private final double centerY;
    private final double radius;

    public DeadZone(double centerX, double centerY, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    /**
     * Checks if a flight path between two points intersects this deadzone.
     * Uses line-circle intersection math from the provided reference:
     * https://www.superprof.co.uk/resources/academic/maths/analytical-geometry/conics/circle-line-intersection.html
     */
    public boolean intersectsPath(Location start, Location end) {
        // Treat all movement as occurring at z=50m (flight altitude)
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        // Vector math for line-circle intersection
        double a = dx*dx + dy*dy;
        double b = 2 * (dx*(start.getX() - centerX) + dy*(start.getY() - centerY));
        double c = (start.getX() - centerX)*(start.getX() - centerX)
                + (start.getY() - centerY)*(start.getY() - centerY)
                - radius*radius;

        double discriminant = b*b - 4*a*c;

        // Real roots mean intersection exists
        return discriminant >= 0;
    }

    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getRadius() { return radius; }
}