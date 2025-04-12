package main.java.core.utils;

import main.java.core.models.Location;

import java.util.List;
import java.util.StringJoiner;

/**
 * Formats paths into the submission format required by the competition.
 * The submission format requires a list of drone runs, which are lists of 2D coordinates.
 * Format: [[(x0,y0),(x1,y1),...,(x0,y0)],[(x0,y0),(x1,y1),...,(x0,y0)]]
 */
public class OutputFormatter {
    
    /**
     * Convert a list of paths to the required submission format.
     * Each path represents one battery swap and must start/end with the depot.
     * 
     * @param paths List of paths (each representing one battery)
     * @return Formatted string ready for submission
     */
    public String formatForSubmission(List<List<Location>> paths) {
        if (paths == null || paths.isEmpty()) {
            return "[]";
        }
        
        StringJoiner pathsJoiner = new StringJoiner(",", "[", "]");
        
        for (List<Location> path : paths) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            
            StringJoiner coordsJoiner = new StringJoiner(",", "[", "]");
            
            for (Location location : path) {
                // Only include x and y coordinates per specification (z is calculated by submission engine)
                String coordStr = "(" + location.getX() + "," + location.getY() + ")";
                coordsJoiner.add(coordStr);
            }
            
            pathsJoiner.add(coordsJoiner.toString());
        }
        
        return pathsJoiner.toString();
    }
    
    /**
     * Convert a single path to the required submission format.
     * Useful for Level 1 where only one battery is used.
     * 
     * @param path A single path (must start/end with depot)
     * @return Formatted string ready for submission
     */
    public String formatSinglePathForSubmission(List<Location> path) {
        if (path == null || path.isEmpty()) {
            return "[[]]";
        }
        
        StringJoiner coordsJoiner = new StringJoiner(",", "[", "]");
        
        for (Location location : path) {
            // Only include x and y coordinates per specification
            String coordStr = "(" + location.getX() + "," + location.getY() + ")";
            coordsJoiner.add(coordStr);
        }
        
        return "[" + coordsJoiner.toString() + "]";
    }
    
    /**
     * Validate that a path adheres to the competition rules:
     * - Must start and end at the same depot location
     * - Must not be empty
     * 
     * @param path The path to validate
     * @return true if the path is valid, false otherwise
     */
    public boolean isValidPath(List<Location> path) {
        if (path == null || path.size() < 2) {
            return false;
        }
        
        Location start = path.get(0);
        Location end = path.get(path.size() - 1);
        
        // Check that the path starts and ends at the same location (depot)
        return start.getX() == end.getX() && start.getY() == end.getY();
    }
    
    /**
     * Validate that all paths adhere to the competition rules
     * 
     * @param paths List of paths to validate
     * @return true if all paths are valid, false otherwise
     */
    public boolean areValidPaths(List<List<Location>> paths) {
        if (paths == null || paths.isEmpty()) {
            return false;
        }
        
        for (List<Location> path : paths) {
            if (!isValidPath(path)) {
                return false;
            }
        }
        
        return true;
    }
}