/**
 * InputParser - Utility class for parsing zoo configuration files
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
package core.utils;

package core.utils;

import core.models.*;
import core.services.ZooMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputParser {
    // Patterns for parsing different coordinate formats
    private static final Pattern COORD_PATTERN = Pattern.compile("\\((\\d+),(\\d+),(\\d+)(?:,(\\w+))?(?:,(\\d+\\.?\\d*))?\\)");
    private static final Pattern DEADZONE_PATTERN = Pattern.compile("\\((\\d+),(\\d+),(\\d+)\\)");

    // Main method to parse the input file and construct ZooMap
    public static ZooMap parseInputFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int[] dimensions = null;
        Depot depot = null;
        int batteryCapacity = 0;
        List<FoodStorage> foodStorages = new ArrayList<>();
        List<AnimalEnclosure> enclosures = new ArrayList<>();
        List<Deadzone> deadzones = new ArrayList<>();

        // Reading file line by line
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Handling different sections of the input file
            if (line.startsWith("ZOO DIMENSIONS")) {
                dimensions = parseCoordinates(line.substring(line.indexOf("(")));
            }
            else if (line.startsWith("DRONE DEPOT")) {
                int[] coords = parseCoordinates(line.substring(line.indexOf("(")));
                depot = new Depot(coords[0], coords[1], coords[2]);
            }
            else if (line.startsWith("BATTERY DISTANCE CAPACITY")) {
                batteryCapacity = Integer.parseInt(line.substring(line.indexOf("(") + 1, line.indexOf(")")));
            }
            else if (line.startsWith("FOOD STORAGES COORDINATES")) {
                foodStorages.addAll(parseFoodStorages(line.substring(line.indexOf("["))));
            }
            else if (line.startsWith("ENCLOSURES")) {
                enclosures.addAll(parseEnclosures(line.substring(line.indexOf("["))));
            }
            else if (line.startsWith("DEADZONES")) {
                deadzones.addAll(parseDeadzones(line.substring(line.indexOf("["))));
            }
        }
        reader.close();

        // Validation check for required fields
        if (dimensions == null || depot == null) {
            throw new IllegalArgumentException("Invalid input file: missing required fields");
        }

        return new ZooMap(dimensions[0], dimensions[1], dimensions[2], depot,
                batteryCapacity, foodStorages, enclosures, deadzones);
    }

    // Parses basic coordinate triplets (x,y,z)
    private static int[] parseCoordinates(String coordString) {
        Matcher matcher = COORD_PATTERN.matcher(coordString);
        if (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            return new int[]{x, y, z};
        }
        throw new IllegalArgumentException("Invalid coordinate format: " + coordString);
    }

    // Parses food storage locations with diet information
    private static List<FoodStorage> parseFoodStorages(String storageString) {
        List<FoodStorage> storages = new ArrayList<>();
        Matcher matcher = COORD_PATTERN.matcher(storageString);

        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            char diet = matcher.group(4).charAt(0);
            storages.add(new FoodStorage(x, y, z, diet));
        }
        return storages;
    }

    // Parses animal enclosures with importance and diet
    private static List<AnimalEnclosure> parseEnclosures(String enclosureString) {
        List<AnimalEnclosure> enclosures = new ArrayList<>();
        Matcher matcher = COORD_PATTERN.matcher(enclosureString);

        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            float importance = Float.parseFloat(matcher.group(5));
            char diet = matcher.group(4).charAt(0);
            enclosures.add(new AnimalEnclosure(x, y, z, importance, diet));
        }
        return enclosures;
    }

    // Parses deadzone areas with radius
    private static List<Deadzone> parseDeadzones(String deadzoneString) {
        List<Deadzone> deadzones = new ArrayList<>();
        Matcher matcher = DEADZONE_PATTERN.matcher(deadzoneString);

        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int radius = Integer.parseInt(matcher.group(3));
            deadzones.add(new Deadzone(x, y, radius));
        }
        return deadzones;
    }
}