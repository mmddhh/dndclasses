package mdh.dndclasses.worldgen.road;

/** Terrain cost adapter supplied by the Minecraft layer; the planner itself stays pure Java. */
public interface TerrainCost {

    boolean passable(int x, int z);

    int height(int x, int z);

    /** Cost of moving from one sample cell to an adjacent sample cell. */
    double moveCost(int fromX, int fromZ, int toX, int toZ);
}
