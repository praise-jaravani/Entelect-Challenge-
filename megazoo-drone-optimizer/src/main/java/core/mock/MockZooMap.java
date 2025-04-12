package main.java.core.mock;

import core.models.AnimalEnclosure;
import core.models.Deadzone;
import core.models.Depot;
import core.models.FoodStorage;
import core.models.Location;
import core.services.ZooMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mock implementation of the ZooMap interface for testing purposes.
 * Represents the zoo layout with all locations (depot, food storages, enclosures, deadzones).
 */
public class MockZooMap implements ZooMap {
    private final int width;
    private final int height;
    private final int maxHeight;
    private final Depot depot;
    private final List<FoodStorage> foodStorages = new ArrayList<>();
    private final List<AnimalEnclosure> enclosures = new ArrayList<>();
    private final List<Deadzone> deadzones = new ArrayList<>();
    
    /**
     * Create a new mock zoo map with the specified dimensions.
     *
     * @param width width of the zoo
     * @param height height of the zoo
     * @param maxHeight maximum flying height
     * @param depot the drone depot
     */
    public MockZooMap(int width, int height, int maxHeight, Depot depot) {
        this.width = width;
        this.height = height;
        this.maxHeight = maxHeight;
        this.depot = depot;
    }
    
    /**
     * Add a food storage to the zoo.
     *
     * @param storage the food storage to add
     */
    public void addFoodStorage(FoodStorage storage) {
        foodStorages.add(storage);
    }
    
    /**
     * Add an animal enclosure to the zoo.
     *
     * @param enclosure the animal enclosure to add
     */
    public void addEnclosure(AnimalEnclosure enclosure) {
        enclosures.add(enclosure);
    }
    
    /**
     * Add a deadzone to the zoo.
     *
     * @param deadzone the deadzone to add
     */
    public void addDeadzone(Deadzone deadzone) {
        deadzones.add(deadzone);
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public int getMaxHeight() {
        return maxHeight;
    }
    
    @Override
    public Depot getDepot() {
        return depot;
    }
    
    @Override
    public List<FoodStorage> getAllFoodStorages() {
        return Collections.unmodifiableList(foodStorages);
    }
    
    @Override
    public List<FoodStorage> getFoodStoragesByType(char foodType) {
        return foodStorages.stream()
                .filter(storage -> storage.getFoodType() == foodType)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AnimalEnclosure> getAllEnclosures() {
        return Collections.unmodifiableList(enclosures);
    }
    
    @Override
    public List<AnimalEnclosure> getEnclosuresByDiet(char diet) {
        return enclosures.stream()
                .filter(enclosure -> enclosure.getDiet() == diet)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Deadzone> getAllDeadzones() {
        return Collections.unmodifiableList(deadzones);
    }
    
    @Override
    public FoodStorage getNearestFoodStorage(Location location, char foodType) {
        List<FoodStorage> matchingStorages = getFoodStoragesByType(foodType);
        
        if (matchingStorages.isEmpty()) {
            return null;
        }
        
        FoodStorage nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (FoodStorage storage : matchingStorages) {
            double distance = calculateDistance(location, storage);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = storage;
            }
        }
        
        return nearest;
    }
    
    /**
     * Simple Euclidean distance calculation between two locations.
     */
    private double calculateDistance(Location a, Location b) {
        return Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2) +
            Math.pow(a.getY() - b.getY(), 2) +
            Math.pow(a.getZ() - b.getZ(), 2)
        );
    }
    
    /**
     * Create a simple test zoo for Level 1.
     * 
     * @return a MockZooMap configured for testing Level 1
     */
    public static MockZooMap createTestZooLevel1() {
        // Create a depot at the center of the zoo
        MockDepot depot = new MockDepot(50, 50, 10);
        
        // Create the zoo map (100x100 with max height 50)
        MockZooMap zooMap = new MockZooMap(100, 100, 50, depot);
        
        // Add one food storage of each type
        zooMap.addFoodStorage(new MockFoodStorage(30, 30, 5, 'c'));
        zooMap.addFoodStorage(new MockFoodStorage(50, 20, 5, 'h'));
        zooMap.addFoodStorage(new MockFoodStorage(70, 30, 5, 'o'));
        
        // Add 20 animal enclosures (different diets and importance values)
        // Carnivores
        zooMap.addEnclosure(new MockAnimalEnclosure(20, 40, 5, 2.5, 'c'));
        zooMap.addEnclosure(new MockAnimalEnclosure(25, 65, 5, 1.8, 'c'));
        zooMap.addEnclosure(new MockAnimalEnclosure(30, 80, 5, 3.2, 'c'));
        zooMap.addEnclosure(new MockAnimalEnclosure(40, 75, 5, 1.5, 'c'));
        zooMap.addEnclosure(new MockAnimalEnclosure(45, 85, 5, 4.0, 'c'));
        
        // Herbivores
        zooMap.addEnclosure(new MockAnimalEnclosure(60, 75, 5, 2.0, 'h'));
        zooMap.addEnclosure(new MockAnimalEnclosure(65, 85, 5, 1.3, 'h'));
        zooMap.addEnclosure(new MockAnimalEnclosure(70, 70, 5, 3.7, 'h'));
        zooMap.addEnclosure(new MockAnimalEnclosure(75, 55, 5, 2.2, 'h'));
        zooMap.addEnclosure(new MockAnimalEnclosure(80, 65, 5, 1.9, 'h'));
        zooMap.addEnclosure(new MockAnimalEnclosure(85, 45, 5, 3.5, 'h'));
        zooMap.addEnclosure(new MockAnimalEnclosure(90, 35, 5, 2.8, 'h'));
        
        // Omnivores
        zooMap.addEnclosure(new MockAnimalEnclosure(15, 15, 5, 2.1, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(25, 25, 5, 3.3, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(35, 15, 5, 1.7, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(45, 25, 5, 2.4, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(55, 15, 5, 3.9, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(65, 25, 5, 1.2, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(75, 15, 5, 2.6, 'o'));
        zooMap.addEnclosure(new MockAnimalEnclosure(85, 25, 5, 3.1, 'o'));
        
        return zooMap;
    }
}