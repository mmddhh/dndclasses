package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block-space river lookup built from the same D8 paths as the map. It is installed by the map /
 * hydrology command so chunk generation can carve the identical channels.
 */
public final class RiverField {

    public record Hit(int widthBlocks, double distance) {
    }

    private record Segment(double x1, double z1, double x2, double z2, int width) {
    }

    private static volatile RiverField current;

    private final int cellSize;
    private final int side;
    private final int gridSize;
    private final Map<Long, List<Segment>> buckets;

    private RiverField(int cellSize, int side, int gridSize, Map<Long, List<Segment>> buckets) {
        this.cellSize = cellSize;
        this.side = side;
        this.gridSize = gridSize;
        this.buckets = buckets;
    }

    public static void install(List<RiverPath> paths, WorldTopology topology) {
        int cellSize = topology.cellSize();
        int side = topology.sideBlocks();
        int gridSize = topology.gridSize();
        Map<Long, List<Segment>> buckets = new HashMap<>();
        for (RiverPath path : paths) {
            List<RiverPath.Node> points = path.points();
            for (int i = 1; i < points.size(); i++) {
                RiverPath.Node a = points.get(i - 1);
                RiverPath.Node b = points.get(i);
                Segment segment = new Segment(
                        (a.x() + 0.5D) * cellSize,
                        (a.z() + 0.5D) * cellSize,
                        (b.x() + 0.5D) * cellSize,
                        (b.z() + 0.5D) * cellSize,
                        path.width());
                long keyA = bucketKey(a.x(), a.z(), cellSize, side);
                long keyB = bucketKey(b.x(), b.z(), cellSize, side);
                buckets.computeIfAbsent(keyA, ignored -> new ArrayList<>()).add(segment);
                if (keyB != keyA) {
                    buckets.computeIfAbsent(keyB, ignored -> new ArrayList<>()).add(segment);
                }
            }
        }
        current = new RiverField(cellSize, side, gridSize, buckets);
    }

    public static RiverField current() {
        return current;
    }

    /** Returns the nearest channel if the point is inside its watered width. */
    public static Hit hit(int x, int z) {
        RiverField field = current;
        if (field == null) {
            return null;
        }
        int cx = Math.floorMod(x, field.side) / field.cellSize;
        int cz = Math.floorMod(z, field.side) / field.cellSize;
        Hit best = null;
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = Math.floorMod(cx + dx, field.gridSize);
                int nz = Math.floorMod(cz + dz, field.gridSize);
                List<Segment> segments = field.buckets.get(bucketKey(nx, nz, field.cellSize, field.side));
                if (segments == null) {
                    continue;
                }
                for (Segment segment : segments) {
                    double distance = distanceToSegmentSq(x, z, segment);
                    double radius = segment.width() * 0.5D;
                    if (distance <= radius * radius) {
                        if (best == null || distance < best.distance()) {
                            best = new Hit(segment.width(), Math.sqrt(distance));
                        }
                    }
                }
            }
        }
        return best;
    }

    private static double distanceToSegmentSq(int px, int pz, Segment segment) {
        double dx = segment.x2() - segment.x1();
        double dz = segment.z2() - segment.z1();
        double lengthSq = dx * dx + dz * dz;
        double t = lengthSq == 0.0D ? 0.0D
                : (((px - segment.x1()) * dx + (pz - segment.z1()) * dz) / lengthSq);
        t = Math.max(0.0D, Math.min(1.0D, t));
        double x = segment.x1() + t * dx;
        double z = segment.z1() + t * dz;
        return (px - x) * (px - x) + (pz - z) * (pz - z);
    }

    private static long bucketKey(int worldX, int worldZ, int cellSize, int side) {
        int cx = Math.floorMod(worldX, side) / cellSize;
        int cz = Math.floorMod(worldZ, side) / cellSize;
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }
}
