package main.java;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

// Use these imports if your classes are in src/main/java/core/...
import main.java.core.models.AnimalEnclosure;
import main.java.core.services.ZooMap;
import main.java.core.utils.InputParser;
import main.java.levels.*;

/**
 * Main entry point for the Megazoo Drone Optimizer.
 * Handles command line arguments, loads zoo configurations, and runs the appropriate solver.
 */
public class Main {
    
    /**
     * Main method to run the Megazoo Drone Optimizer.
     * 
     * Usage: java Main <level> <input_file> <output_file>
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Validate command line arguments
        if (args.length < 2) {
            System.out.println("Usage: java Main <level> <input_file> [output_file]");
            System.out.println("  level: 1, 2, 3, or 4");
            System.out.println("  input_file: Path to the zoo configuration file");
            System.out.println("  output_file: Optional path to save the solution (defaults to 'solution.txt')");
            return;
        }
        
        // Parse arguments
        int level;
        try {
            level = Integer.parseInt(args[0]);
            if (level < 1 || level > 4) {
                throw new IllegalArgumentException("Level must be between 1 and 4");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Level must be a number (1-4)");
            return;
        }
        
        String inputFile = args[1];
        String outputFile = args.length > 2 ? args[2] : "solution.txt";
        
        try {
            // Parse the input file to create the zoo map
            System.out.println("Parsing input file: " + inputFile);
            ZooMap zooMap = InputParser.parseInputFile(inputFile);
            System.out.println("Zoo loaded successfully");
            
            // Print basic zoo information
            printZooInfo(zooMap, level);
            
            // Create and run the appropriate solver
            LevelSolver solver = createSolverForLevel(level, zooMap);
            
            // Generate and save the solution
            System.out.println("Generating solution...");
            String solution = solver.generateSubmission();
            
            // Print some statistics about the solution
            printSolutionStats(solver);
            
            // Save the solution to the output file
            saveToFile(solution, outputFile);
            System.out.println("Solution saved to: " + outputFile);
            
        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the appropriate solver for the specified level.
     * 
     * @param level The level to solve
     * @param zooMap The parsed zoo map
     * @return The appropriate LevelSolver
     */
    private static LevelSolver createSolverForLevel(int level, ZooMap zooMap) {
        System.out.println("Creating solver for Level " + level);
        
        switch (level) {
            case 1:
                return new Level1Solver(zooMap);
            case 2:
                return new Level2Solver(zooMap);
            case 3:
                return new Level3Solver(zooMap);
            case 4:
                return new Level4Solver(zooMap);
            default:
                throw new IllegalArgumentException("Invalid level: " + level);
        }
    }
    
    /**
     * Prints basic information about the zoo configuration.
     * 
     * @param zooMap The parsed zoo map
     * @param level The level being solved
     */
    private static void printZooInfo(ZooMap zooMap, int level) {
        System.out.println("Zoo Information (Level " + level + "):");
        System.out.println("  Dimensions: " + zooMap.getWidth() + "m x " + zooMap.getHeight() + "m x " + zooMap.getMaxHeight() + "m");
        System.out.println("  Depot location: (" + zooMap.getDepot().getX() + "," + zooMap.getDepot().getY() + "," + zooMap.getDepot().getZ() + ")");
        System.out.println("  Total enclosures: " + zooMap.getAllEnclosures().size());
        System.out.println("  Food storages: " + zooMap.getAllFoodStorages().size());
        System.out.println("  Deadzones: " + zooMap.getAllDeadzones().size());
    }
    
    /**
     * Prints statistics about the generated solution.
     * 
     * @param solver The solver that generated the solution
     */
    private static void printSolutionStats(LevelSolver solver) {
        System.out.println("Solution Statistics:");
        
        if (solver instanceof Level1Solver) {
            System.out.println(((Level1Solver) solver).getScoreDetails());
        } else if (solver instanceof Level2Solver) {
            System.out.println(((Level2Solver) solver).getScoreDetails());
            System.out.println(((Level2Solver) solver).getDietDistribution());
        } else if (solver instanceof Level3Solver) {
            System.out.println(((Level3Solver) solver).getScoreDetails());
            System.out.println("Has deadzone intersections: " + ((Level3Solver) solver).hasDeadzoneIntersections());
        } else if (solver instanceof Level4Solver) {
            System.out.println(((Level4Solver) solver).getScoreDetails());
            System.out.println(((Level4Solver) solver).getEfficiencyMetrics());
        }
    }
    
    /**
     * Saves a string to a file.
     * 
     * @param content The content to save
     * @param filename The filename to save to
     * @throws IOException If there is an error writing to the file
     */
    private static void saveToFile(String content, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}