#!/bin/bash

# Script to set up the file structure for Megazoo Drone Optimizer project

# Create main directory structure
mkdir -p megazoo-drone-optimizer/src/main/java/core/{models,services,utils,algorithm,mock}
mkdir -p megazoo-drone-optimizer/src/main/java/levels
mkdir -p megazoo-drone-optimizer/src/main/resources
mkdir -p megazoo-drone-optimizer/src/test/java/core/{models,services,utils,algorithm}
mkdir -p megazoo-drone-optimizer/src/test/java/levels

# Create main class
touch megazoo-drone-optimizer/src/main/java/Main.java

# Create model classes (Person 1)
touch megazoo-drone-optimizer/src/main/java/core/models/Location.java
touch megazoo-drone-optimizer/src/main/java/core/models/Depot.java
touch megazoo-drone-optimizer/src/main/java/core/models/FoodStorage.java
touch megazoo-drone-optimizer/src/main/java/core/models/AnimalEnclosure.java
touch megazoo-drone-optimizer/src/main/java/core/models/Deadzone.java
touch megazoo-drone-optimizer/src/main/java/core/models/Drone.java

# Create service classes (Person 1)
touch megazoo-drone-optimizer/src/main/java/core/services/DistanceCalculator.java
touch megazoo-drone-optimizer/src/main/java/core/services/PathValidator.java
touch megazoo-drone-optimizer/src/main/java/core/services/ZooMap.java

# Create utility classes
touch megazoo-drone-optimizer/src/main/java/core/utils/InputParser.java  # Person 1
touch megazoo-drone-optimizer/src/main/java/core/utils/OutputFormatter.java  # Person 2

# Create algorithm classes (Person 2)
touch megazoo-drone-optimizer/src/main/java/core/algorithm/PathPlanner.java
touch megazoo-drone-optimizer/src/main/java/core/algorithm/GreedyPathPlanner.java
touch megazoo-drone-optimizer/src/main/java/core/algorithm/ClusterPathPlanner.java
touch megazoo-drone-optimizer/src/main/java/core/algorithm/RouteOptimizer.java
touch megazoo-drone-optimizer/src/main/java/core/algorithm/ScoreCalculator.java

# Create mock classes (Person 2 temporary implementations)
touch megazoo-drone-optimizer/src/main/java/core/mock/MockLocation.java
touch megazoo-drone-optimizer/src/main/java/core/mock/MockDepot.java
touch megazoo-drone-optimizer/src/main/java/core/mock/MockFoodStorage.java
touch megazoo-drone-optimizer/src/main/java/core/mock/MockAnimalEnclosure.java
touch megazoo-drone-optimizer/src/main/java/core/mock/MockZooMap.java
touch megazoo-drone-optimizer/src/main/java/core/mock/MockDistanceCalculator.java

# Create level solver classes
touch megazoo-drone-optimizer/src/main/java/levels/LevelSolver.java
touch megazoo-drone-optimizer/src/main/java/levels/Level1Solver.java
touch megazoo-drone-optimizer/src/main/java/levels/Level2Solver.java
touch megazoo-drone-optimizer/src/main/java/levels/Level3Solver.java
touch megazoo-drone-optimizer/src/main/java/levels/Level4Solver.java

# Create test files
touch megazoo-drone-optimizer/src/test/java/core/models/DroneTest.java
touch megazoo-drone-optimizer/src/test/java/core/services/DistanceCalculatorTest.java
touch megazoo-drone-optimizer/src/test/java/core/algorithm/GreedyPathPlannerTest.java
touch megazoo-drone-optimizer/src/test/java/core/algorithm/RouteOptimizerTest.java
touch megazoo-drone-optimizer/src/test/java/levels/Level1SolverTest.java

# Create resource files
touch megazoo-drone-optimizer/src/main/resources/level1.txt
touch megazoo-drone-optimizer/src/main/resources/level2.txt
touch megazoo-drone-optimizer/src/main/resources/level3.txt
touch megazoo-drone-optimizer/src/main/resources/level4.txt

# Create pom.xml and README
touch megazoo-drone-optimizer/pom.xml
touch megazoo-drone-optimizer/README.md

echo "Project structure created successfully!"
echo "Directory structure:"
find megazoo-drone-optimizer -type d | sort

echo "Files created:"
find megazoo-drone-optimizer -type f | sort