package mdh.dndclasses.worldgen.terrain;

/**
 * Square torus coordinate contract for the larger world.
 *
 * <p>Both axes wrap. The canonical player-facing coordinates are {@code [0, side)}; a centered
 * coordinate can be converted with {@link #center(int)}. Climate uses the z coordinate as a folded
 * latitude: z=0 is the southern cold belt, z=side/4 and z=3*side/4 are the two symmetric warm
 * equators, and z=side/2 is the northern cold seam.
 */
public record WorldTopology(int sideBlocks, int cellSize) {

    public static final int DEFAULT_SIDE = 62_720;
    public static final int DEFAULT_CELL = 256;

    public WorldTopology {
        if (sideBlocks <= 0 || cellSize <= 0 || sideBlocks % cellSize != 0) {
            throw new IllegalArgumentException("side must be a positive multiple of cellSize");
        }
    }

    public static WorldTopology standard() {
        return new WorldTopology(DEFAULT_SIDE, DEFAULT_CELL);
    }

    public int gridSize() {
        return sideBlocks / cellSize;
    }

    /** Canonical coordinate in {@code [0, side)}. */
    public int wrap(int coordinate) {
        return Math.floorMod(coordinate, sideBlocks);
    }

    /** Centered coordinate in {@code [-side/2, side/2)}. */
    public int center(int coordinate) {
        int wrapped = wrap(coordinate);
        return wrapped >= sideBlocks / 2 ? wrapped - sideBlocks : wrapped;
    }

    /** Shortest signed delta from a to b on the torus. */
    public int delta(int a, int b) {
        int difference = wrap(b - a);
        if (difference > sideBlocks / 2) {
            difference -= sideBlocks;
        }
        return difference;
    }

    public int distance(int a, int b) {
        return Math.abs(delta(a, b));
    }

    /** Center coordinate of a grid cell, canonical range. */
    public int cellCenter(int cellIndex) {
        return cellIndex * cellSize + cellSize / 2;
    }

    public int cellCoordinate(int worldCoordinate) {
        return wrap(worldCoordinate) / cellSize;
    }
}
