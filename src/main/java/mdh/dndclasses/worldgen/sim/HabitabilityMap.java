package mdh.dndclasses.worldgen.sim;

/** Minecraft-free habitability grid handed from the world sampler to the civilization simulator. */
public record HabitabilityMap(long seed, int gridSize, int cellSize,
                              double[][] habitability, boolean[][] water) {
}
