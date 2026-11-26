package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * TFC-style lazy regional hydrology: a 4096-block region is sampled, drained and cached on demand.
 * The chunk generator never needs a globally baked river network.
 */
public final class RegionalHydrology {

    private static final int REGION_BLOCKS = 4_096;
    private static final int CELL_BLOCKS = 64;
    private static final WorldTopology TOPOLOGY = WorldTopology.standard();
    private static final int SIDE = TOPOLOGY.sideBlocks();
    private static final int REGIONS = (SIDE + REGION_BLOCKS - 1) / REGION_BLOCKS;
    private static final int EDGE_MARGIN = CELL_BLOCKS * 4;
    private static final int BUCKET_BLOCKS = 128;
    private static final int CACHE_SIZE = 64;

    public record Hit(int widthBlocks, double distance, int waterLevel) {
    }

    private record Segment(double x1, double z1, double x2, double z2, int width,
                           double startLevel, double endLevel) {
    }

    private record RegionKey(long seed, int x, int z) {
    }

    private record Region(Map<Long, List<Segment>> buckets) {
        List<Segment> near(int x, int z) {
            return buckets.getOrDefault(key(Math.floorDiv(x, BUCKET_BLOCKS),
                    Math.floorDiv(z, BUCKET_BLOCKS)), List.of());
        }
    }

    private static final Map<RegionKey, CompletableFuture<Region>> CACHE =
            new LinkedHashMap<>(CACHE_SIZE, 0.75f, true);

    private RegionalHydrology() {
    }

    public static Hit hit(int x, int z, long seed) {
        int wx = TOPOLOGY.wrap(x);
        int wz = TOPOLOGY.wrap(z);
        Segment best = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        double bestT = 0.0D;
        for (int rz = regionAt(wz - EDGE_MARGIN); rz <= regionAt(wz + EDGE_MARGIN); rz++) {
            for (int rx = regionAt(wx - EDGE_MARGIN); rx <= regionAt(wx + EDGE_MARGIN); rx++) {
                int px = wx - Math.floorDiv(rx, REGIONS) * SIDE;
                int pz = wz - Math.floorDiv(rz, REGIONS) * SIDE;
                Region region = region(new RegionKey(seed, Math.floorMod(rx, REGIONS),
                        Math.floorMod(rz, REGIONS)));
                for (Segment segment : region.near(px, pz)) {
                    double t = projection(px, pz, segment);
                    double dx = px - (segment.x1() + t * (segment.x2() - segment.x1()));
                    double dz = pz - (segment.z1() + t * (segment.z2() - segment.z1()));
                    double distance = dx * dx + dz * dz;
                    double radius = segment.width() * 0.5D;
                    if (distance <= radius * radius && distance < bestDistanceSq) {
                        best = segment;
                        bestDistanceSq = distance;
                        bestT = t;
                    }
                }
            }
        }
        return best == null ? null : new Hit(best.width(), Math.sqrt(bestDistanceSq),
                (int) Math.floor(best.startLevel() + bestT * (best.endLevel() - best.startLevel())));
    }

    private static int regionAt(int coordinate) {
        return Math.floorDiv(coordinate, SIDE) * REGIONS + TOPOLOGY.wrap(coordinate) / REGION_BLOCKS;
    }

    private static CompletableFuture<Region> cachedRegion(RegionKey key, CompletableFuture<Region> pending) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, ignored -> pending);
        }
    }

    private static Region region(RegionKey key) {
        synchronized (CACHE) {
            CompletableFuture<Region> existing = CACHE.get(key);
            if (existing != null && existing.isDone()) {
                return existing.join();
            }
        }
        CompletableFuture<Region> pending = new CompletableFuture<>();
        CompletableFuture<Region> result = cachedRegion(key, pending);
        if (result == pending) {
            try {
                pending.complete(build(key.x(), key.z(), key.seed()));
            } catch (RuntimeException | Error failure) {
                pending.completeExceptionally(failure);
                synchronized (CACHE) {
                    CACHE.remove(key, pending);
                }
                throw failure;
            }
            synchronized (CACHE) {
                var entries = CACHE.entrySet().iterator();
                while (CACHE.size() > CACHE_SIZE && entries.hasNext()) {
                    if (entries.next().getValue().isDone()) {
                        entries.remove();
                    }
                }
            }
        }
        return result.join();
    }

    private static Region build(int regionX, int regionZ, long seed) {
        TerrainDensity density = new TerrainDensity(TOPOLOGY, seed);
        int baseX = regionX * REGION_BLOCKS;
        int baseZ = regionZ * REGION_BLOCKS;
        int width = Math.min(REGION_BLOCKS, SIDE - baseX) / CELL_BLOCKS + 2;
        int depth = Math.min(REGION_BLOCKS, SIDE - baseZ) / CELL_BLOCKS + 2;
        short[] heights = new short[width * depth];
        boolean[] water = new boolean[width * depth];
        for (int gz = 0; gz < depth; gz++) {
            for (int gx = 0; gx < width; gx++) {
                int x = baseX + (gx - 1) * CELL_BLOCKS + CELL_BLOCKS / 2;
                int z = baseZ + (gz - 1) * CELL_BLOCKS + CELL_BLOCKS / 2;
                int height = density.surfaceHeight(x, z);
                int index = gz * width + gx;
                heights[index] = (short) height;
                water[index] = height < TerrainModel.SEA_LEVEL;
            }
        }
        D8Hydrology.Result result = D8Hydrology.computeRegion(width, depth, heights, water);
        Map<Long, List<Segment>> buckets = new HashMap<>();
        for (RiverPath path : result.paths()) {
            int count = path.points().size();
            int[] terrain = new int[count];
            int[] levels = new int[count];
            for (int i = 0; i < count; i++) {
                RiverPath.Node node = path.points().get(i);
                terrain[i] = heights[node.z() * width + node.x()];
            }
            // Backward water profile: a local pit fills to the downstream spill level, while the
            // surface still follows the general terrain slope instead of an artificial constant drop.
            levels[count - 1] = Math.max(TerrainModel.SEA_LEVEL, terrain[count - 1]);
            for (int i = count - 2; i >= 0; i--) {
                int natural = Math.max(TerrainModel.SEA_LEVEL, terrain[i]);
                // A coarse cell represents 64 blocks. Keep ordinary river slope gentle and
                // reserve multi-block drops for explicit waterfalls added later.
                levels[i] = Math.max(levels[i + 1],
                        Math.min(natural, levels[i + 1] + 1));
            }
            for (int i = 1; i < count; i++) {
                RiverPath.Node a = path.points().get(i - 1);
                RiverPath.Node b = path.points().get(i);
                double x1 = baseX + (a.x() - 1 + 0.5D) * CELL_BLOCKS;
                double z1 = baseZ + (a.z() - 1 + 0.5D) * CELL_BLOCKS;
                double x2 = baseX + (b.x() - 1 + 0.5D) * CELL_BLOCKS;
                double z2 = baseZ + (b.z() - 1 + 0.5D) * CELL_BLOCKS;
                double dx = x2 - x1;
                double dz = z2 - z1;
                double length = Math.sqrt(dx * dx + dz * dz);
                double middleLevel = (levels[i - 1] + levels[i]) * 0.5D;
                double meander = Math.sin(i * 0.83D + path.points().get(0).x() * 0.11D)
                        * Math.min(26.0D, length * 0.18D);
                double mx = (x1 + x2) * 0.5D - dz / Math.max(1.0D, length) * meander;
                double mz = (z1 + z2) * 0.5D + dx / Math.max(1.0D, length) * meander;
                if (length == 0.0D) continue;
                int startWidth = channelWidth(result.flow()[a.z() * width + a.x()]);
                int endWidth = channelWidth(result.flow()[b.z() * width + b.x()]);
                index(buckets, new Segment(x1, z1, mx, mz, startWidth, levels[i - 1], middleLevel));
                index(buckets, new Segment(mx, mz, x2, z2, endWidth, middleLevel, levels[i]));
            }
        }
        return new Region(buckets);
    }

    private static int channelWidth(int flow) {
        return Math.min(64, 4 + (int) Math.sqrt(flow));
    }

    private static void index(Map<Long, List<Segment>> buckets, Segment segment) {
        double radius = segment.width() * 0.5D;
        int minX = bucket(Math.min(segment.x1(), segment.x2()) - radius);
        int maxX = bucket(Math.max(segment.x1(), segment.x2()) + radius);
        int minZ = bucket(Math.min(segment.z1(), segment.z2()) - radius);
        int maxZ = bucket(Math.max(segment.z1(), segment.z2()) + radius);
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                buckets.computeIfAbsent(key(x, z), ignored -> new ArrayList<>()).add(segment);
            }
        }
    }

    private static int bucket(double coordinate) {
        return (int) Math.floor(coordinate / BUCKET_BLOCKS);
    }

    private static double projection(int px, int pz, Segment segment) {
        double dx = segment.x2() - segment.x1();
        double dz = segment.z2() - segment.z1();
        double lengthSq = dx * dx + dz * dz;
        double t = lengthSq == 0.0D ? 0.0D
                : (((px - segment.x1()) * dx + (pz - segment.z1()) * dz) / lengthSq);
        return Math.max(0.0D, Math.min(1.0D, t));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
