package mdh.dndclasses.worldgen.terrain;

import java.util.LinkedHashMap;
import java.util.Map;

/** Samples the same terrain column for blocks, biome selection and height queries. */
public final class TerrainColumns {
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 512;
    public static final int SEA_LEVEL = TerrainModel.SEA_LEVEL;

    private static final int STEP = 4;
    private static final int MAX_Y = MIN_Y + HEIGHT - 1;
    private static final int INTERVALS = HEIGHT / STEP;
    private static final WorldTopology TOPOLOGY = WorldTopology.standard();

    public record Column(int ground, int water, boolean river, TerrainModel.Biome biome,
                         double groundTemperature, double waterTemperature) {
    }

    private static final class Cache {
        final Map<Long, double[]> profiles = boundedCache(256);
        final Map<Long, Column> columns = boundedCache(512);
    }

    private final long seed;
    private final TerrainModel model;
    private final TerrainDensity density;
    private final TerrainBiomeResolver resolver;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    public TerrainColumns(long seed) {
        this.seed = seed;
        model = new TerrainModel(TOPOLOGY, seed);
        density = new TerrainDensity(TOPOLOGY, seed);
        resolver = new TerrainBiomeResolver(model, density);
    }

    public long seed() {
        return seed;
    }

    public boolean isCave(int x, int y, int z, Column column) {
        int cover = column.water() > column.ground() ? 14 : 6;
        return y >= MIN_Y + 7 && y <= column.ground() - cover && density.isCave(x, y, z);
    }

    public Column sample(int x, int z) {
        int wx = TOPOLOGY.wrap(x);
        int wz = TOPOLOGY.wrap(z);
        Cache local = cache.get();
        return local.columns.computeIfAbsent(key(wx, wz), ignored -> sample(wx, wz, local));
    }

    private Column sample(int x, int z, Cache local) {
        int ground = surface(x, z, local);
        int water = ground < SEA_LEVEL ? SEA_LEVEL : MIN_Y - 1;
        RegionalHydrology.Hit hit = RegionalHydrology.hit(x, z, seed, ground);
        if (hit != null && hit.lake()) water = Math.max(water, hit.waterLevel());
        boolean river = hit != null && !hit.lake()
                && (ground >= SEA_LEVEL || hit.waterLevel() > SEA_LEVEL);
        if (river) {
            water = hit.waterLevel();
            double across = Math.min(1.0D, hit.distance() * 2.0D / hit.widthBlocks());
            int depth = 2 + (int) Math.round((1.0D - across * across) * Math.min(5, hit.widthBlocks() / 8));
            ground = Math.min(ground, water - depth);
        }
        TerrainModel.Biome biome = resolver.biomeAt(x, z, ground);
        return new Column(ground, water, river, biome, model.temperature(x, z, ground),
                water > ground ? model.temperature(x, z, water) : 0.0D);
    }

    private int surface(int x, int z, Cache local) {
        int x0 = x - x % STEP;
        int z0 = z - z % STEP;
        double tx = (x - x0) / (double) STEP;
        double tz = (z - z0) / (double) STEP;
        double[] a = profile(x0, z0, local);
        double[] b = tx == 0.0D ? a : profile(x0 + STEP, z0, local);
        double[] c = tz == 0.0D ? a : profile(x0, z0 + STEP, local);
        double[] d = tz == 0.0D ? b : tx == 0.0D ? c : profile(x0 + STEP, z0 + STEP, local);
        double upper = blend(a, b, c, d, INTERVALS, tx, tz);
        for (int i = INTERVALS - 1; i >= 0; i--) {
            double lower = blend(a, b, c, d, i, tx, tz);
            if (upper > 0.0D || lower > 0.0D) {
                // The upper endpoint was checked in the previous interval (or lies above MAX_Y).
                for (int offset = STEP - 1; offset >= 0; offset--) {
                    if (lerp(lower, upper, offset / (double) STEP) > 0.0D) {
                        return MIN_Y + i * STEP + offset;
                    }
                }
            }
            upper = lower;
        }
        return MIN_Y;
    }

    private double[] profile(int x, int z, Cache local) {
        int wx = TOPOLOGY.wrap(x);
        int wz = TOPOLOGY.wrap(z);
        return local.profiles.computeIfAbsent(key(wx, wz), ignored -> {
            double[] values = new double[INTERVALS + 1];
            for (int i = 0; i < values.length; i++) {
                values[i] = density.density(wx, Math.min(MAX_Y, MIN_Y + i * STEP), wz);
            }
            return values;
        });
    }

    private static double blend(double[] a, double[] b, double[] c, double[] d,
                                int y, double tx, double tz) {
        return lerp(lerp(a[y], b[y], tx), lerp(c[y], d[y], tx), tz);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static <V> Map<Long, V> boundedCache(int limit) {
        return new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > limit;
            }
        };
    }
}
