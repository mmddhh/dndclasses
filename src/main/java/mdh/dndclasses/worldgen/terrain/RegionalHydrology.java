package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TFC-style lazy regional hydrology: a 4096-block region is sampled, drained and cached on demand.
 * The chunk generator never needs a globally baked river network.
 */
public final class RegionalHydrology {

    private static final int REGION_BLOCKS = 4_096;
    private static final int CELL_BLOCKS = 64;
    private static final int GRID = REGION_BLOCKS / CELL_BLOCKS;
    private static final int CACHE_SIZE = 64;

    public record Hit(int widthBlocks, double distance, int waterLevel) {
    }

    private record Segment(double x1, double z1, double x2, double z2, int width,
                           int waterLevel) {
    }

    private record Region(List<Segment> segments) {
    }

    private static final Map<Long, Region> CACHE = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Region> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private static long cachedSeed = Long.MIN_VALUE;

    private RegionalHydrology() {
    }

    public static Hit hit(int x, int z, long seed) {
        if (seed != cachedSeed) {
            synchronized (CACHE) {
                CACHE.clear();
                cachedSeed = seed;
            }
        }
        int regionX = Math.floorDiv(Math.floorMod(x, 62_720), REGION_BLOCKS);
        int regionZ = Math.floorDiv(Math.floorMod(z, 62_720), REGION_BLOCKS);
        Hit best = null;
        // Build only the current region. Neighbouring regions are used only if already cached,
        // otherwise the first chunk would build nine 4096-block regions synchronously.
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int rx = Math.floorMod(regionX + dx, 16);
                int rz = Math.floorMod(regionZ + dz, 16);
                Region region;
                if (dx == 0 && dz == 0) {
                    synchronized (CACHE) {
                        region = CACHE.computeIfAbsent(key(rx, rz),
                                ignored -> build(rx, rz, seed));
                    }
                } else {
                    synchronized (CACHE) {
                        region = CACHE.get(key(rx, rz));
                    }
                }
                if (region == null) {
                    continue;
                }
                for (Segment segment : region.segments()) {
                    double distance = distanceToSegmentSq(x, z, segment);
                    double radius = segment.width() * 0.5D;
                    if (distance <= radius * radius) {
                        if (best == null || distance < best.distance()) {
                            best = new Hit(segment.width(), Math.sqrt(distance),
                                    segment.waterLevel());
                        }
                    }
                }
            }
        }
        return best;
    }

    private static Region build(int regionX, int regionZ, long seed) {
        WorldTopology topology = WorldTopology.standard();
        TerrainDensity density = new TerrainDensity(topology, seed);
        int expanded = GRID + 2;
        short[] heights = new short[expanded * expanded];
        boolean[] water = new boolean[expanded * expanded];
        int baseX = regionX * REGION_BLOCKS;
        int baseZ = regionZ * REGION_BLOCKS;
        for (int gz = 0; gz < expanded; gz++) {
            for (int gx = 0; gx < expanded; gx++) {
                int x = baseX + (gx - 1) * CELL_BLOCKS + CELL_BLOCKS / 2;
                int z = baseZ + (gz - 1) * CELL_BLOCKS + CELL_BLOCKS / 2;
                int height = density.surfaceHeight(x, z);
                int index = gz * expanded + gx;
                heights[index] = (short) height;
                water[index] = height < TerrainModel.SEA_LEVEL;
            }
        }
        D8Hydrology.Result result = D8Hydrology.compute(expanded, heights, water);
        List<Segment> segments = new ArrayList<>();
        for (RiverPath path : result.paths()) {
            int waterLevel = TerrainModel.MAX_Y;
            for (int i = 1; i < path.points().size(); i++) {
                RiverPath.Node a = path.points().get(i - 1);
                RiverPath.Node b = path.points().get(i);
                int surface = Math.min(
                        heights[Math.max(0, Math.min(expanded - 1, a.z()))
                                * expanded + Math.max(0, Math.min(expanded - 1, a.x()))],
                        heights[Math.max(0, Math.min(expanded - 1, b.z()))
                                * expanded + Math.max(0, Math.min(expanded - 1, b.x()))]);
                waterLevel = Math.min(waterLevel, surface);
                double x1 = baseX + (a.x() - 1 + 0.5D) * CELL_BLOCKS;
                double z1 = baseZ + (a.z() - 1 + 0.5D) * CELL_BLOCKS;
                double x2 = baseX + (b.x() - 1 + 0.5D) * CELL_BLOCKS;
                double z2 = baseZ + (b.z() - 1 + 0.5D) * CELL_BLOCKS;
                double dx = x2 - x1;
                double dz = z2 - z1;
                double length = Math.sqrt(dx * dx + dz * dz);
                double meander = Math.sin(i * 0.83D + path.points().get(0).x() * 0.11D)
                        * Math.min(26.0D, length * 0.18D);
                double mx = (x1 + x2) * 0.5D - dz / Math.max(1.0D, length) * meander;
                double mz = (z1 + z2) * 0.5D + dx / Math.max(1.0D, length) * meander;
                segments.add(new Segment(x1, z1, mx, mz, path.width(), waterLevel));
                segments.add(new Segment(mx, mz, x2, z2, path.width(), waterLevel));
            }
        }
        return new Region(List.copyOf(segments));
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

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
            cachedSeed = Long.MIN_VALUE;
        }
    }
}
