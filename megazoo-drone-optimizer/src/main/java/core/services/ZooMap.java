/**
 * Central spatial database for all zoo locations with query methods
 * optimized for drone pathfinding operations.
 *
 * @author Karabo Motsileng
 * @version 12 April 2025
 */
public class ZooMap {
    private final Depot depot;
    private final List<FoodStorage> foodStorages;
    private final List<AnimalEnclosure> enclosures;
    private final List<DeadZone> deadZones;
    private final Map<Character, List<FoodStorage>> foodStorageByType;

    public ZooMap(Depot depot,
                  List<FoodStorage> foodStorages,
                  List<AnimalEnclosure> enclosures,
                  List<DeadZone> deadZones) {
        this.depot = depot;
        this.foodStorages = new ArrayList<>(foodStorages);
        this.enclosures = new ArrayList<>(enclosures);
        this.deadZones = new ArrayList<>(deadZones);

        // Pre-index food storages by type for O(1) access
        this.foodStorageByType = new HashMap<>();
        foodStorageByType.put('c', new ArrayList<>());
        foodStorageByType.put('h', new ArrayList<>());
        foodStorageByType.put('o', new ArrayList<>());
        for (FoodStorage storage : foodStorages) {
            foodStorageByType.get(storage.getDietType()).add(storage);
        }
    }

    /**
     * Finds nearest unfed enclosure matching drone's current food type.
     */
    public Optional<AnimalEnclosure> findNearestEligibleEnclosure(Location currentPos, char foodType) {
        return enclosures.stream()
                .filter(e -> !e.isFed() && e.getDietType() == foodType)
                .min(Comparator.comparingDouble(e ->
                        DistanceCalculator.calculateMovementDistance(currentPos, e)));
    }

    /**
     * Finds nearest food storage of specified type.
     */
    public Optional<FoodStorage> findNearestFoodStorage(Location currentPos, char foodType) {
        return foodStorageByType.get(foodType).stream()
                .min(Comparator.comparingDouble(fs ->
                        DistanceCalculator.calculateMovementDistance(currentPos, fs)));
    }

    /**
     * Checks if path between two points intersects any deadzone.
     */
    public boolean isPathSafe(Location start, Location end) {
        return deadZones.stream()
                .noneMatch(dz -> dz.intersectsPath(start, end));
    }

    // Accessors
    public Depot getDepot() { return depot; }
    public List<AnimalEnclosure> getEnclosures() { return Collections.unmodifiableList(enclosures); }
}