package mdh.dndclasses.worldgen.terrain;

/**
 * 3D terrain density. A clean-room implementation: continental mass, erosion and ridged mountains
 * are all sampled as 3D fields, with domain warping to avoid radially symmetric contour rings.
 * No third-party source code is copied.
 */
public final class TerrainDensity {

    private static final int SEA_LEVEL = 63;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 319;

    private final WorldTopology topology;
    private final long seed;
    private final int side;

    public TerrainDensity(WorldTopology topology, long seed) {
        this.topology = topology;
        this.seed = seed;
        this.side = topology.sideBlocks();
    }

    /** Positive is solid, negative is air or water. */
    public double density(int x, int y, int z) {
        int wx = topology.wrap(x);
        int wz = topology.wrap(z);

        double warpX = (noise2(wx, wz, 48, 0x51ED270BL) - 0.5D) * 190.0D;
        double warpZ = (noise2(wx + 3_177, wz - 7_133, 48, 0x6A09E667L) - 0.5D) * 190.0D;
        double px = wx + warpX;
        double pz = wz + warpZ;

        // Broad continental mass: 3D, so its iso-surface is not a single 2D height column.
        double continent = fbm3(px, y, pz, 56, 4, 0x243F6A88L);
        double erosion = fbm3(px + 1_219, y * 1.7D, pz - 2_731, 160, 4, 0x85A308D3L);
        double ridge = ridged3(px, y, pz, 320, 3, 0x13198A2EL);

        double vertical = (SEA_LEVEL - y) / 74.0D;
        double relief = (continent - 0.5D) * 2.0D * 1.20D
                + (erosion - 0.5D) * 0.42D
                + ridge * ridge * 0.85D * (0.35D + erosion * 0.65D);
        return vertical + relief;
    }

    /** Highest solid block at a column, used by the adapter and by land-spawn selection. */
    public int surfaceHeight(int x, int z) {
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            if (density(x, y, z) > 0.0D) {
                return y;
            }
        }
        return MIN_Y;
    }

    public boolean isLand(int x, int z) {
        return surfaceHeight(x, z) >= SEA_LEVEL + 2;
    }

    private double fbm3(double x, double y, double z, int period, int octaves, long salt) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double normal = 0.0D;
        int currentPeriod = period;
        for (int i = 0; i < octaves; i++) {
            total += (noise3(x, y, z, currentPeriod, salt + i * 0x9E3779B9L) - 0.5D)
                    * 2.0D * amplitude;
            normal += amplitude;
            amplitude *= 0.5D;
            currentPeriod *= 2;
        }
        return total / normal * 0.5D + 0.5D;
    }

    private double ridged3(double x, double y, double z, int period, int octaves, long salt) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double normal = 0.0D;
        int currentPeriod = period;
        for (int i = 0; i < octaves; i++) {
            double n = noise3(x, y, z, currentPeriod, salt + i * 0x7F4A7C15L);
            double ridge = 1.0D - Math.abs(n * 2.0D - 1.0D);
            total += ridge * ridge * amplitude;
            normal += amplitude;
            amplitude *= 0.5D;
            currentPeriod *= 2;
        }
        return normal == 0.0D ? 0.0D : total / normal;
    }

    private double noise2(int x, int z, int period, long salt) {
        double scale = period / (double) side;
        double u = x * scale;
        double v = z * scale;
        int x0 = (int) Math.floor(u);
        int z0 = (int) Math.floor(v);
        double tx = smooth(u - x0);
        double tz = smooth(v - z0);
        double a = value2(x0, z0, period, salt);
        double b = value2(x0 + 1, z0, period, salt);
        double c = value2(x0, z0 + 1, period, salt);
        double d = value2(x0 + 1, z0 + 1, period, salt);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private double noise3(double x, double y, double z, int period, long salt) {
        double scale = period / (double) side;
        double u = x * scale;
        double v = z * scale;
        double w = y * scale;
        int x0 = (int) Math.floor(u);
        int z0 = (int) Math.floor(v);
        int y0 = (int) Math.floor(w);
        double tx = smooth(u - x0);
        double tz = smooth(v - z0);
        double ty = smooth(w - y0);
        double a = value3(x0, y0, z0, period, salt);
        double b = value3(x0 + 1, y0, z0, period, salt);
        double c = value3(x0, y0, z0 + 1, period, salt);
        double d = value3(x0 + 1, y0, z0 + 1, period, salt);
        double e = value3(x0, y0 + 1, z0, period, salt);
        double f = value3(x0 + 1, y0 + 1, z0, period, salt);
        double g = value3(x0, y0 + 1, z0 + 1, period, salt);
        double h = value3(x0 + 1, y0 + 1, z0 + 1, period, salt);
        double lower = lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
        double upper = lerp(lerp(e, f, tx), lerp(g, h, tx), tz);
        return lerp(lower, upper, ty);
    }

    private double value2(int x, int z, int period, long salt) {
        long hash = seed ^ salt;
        hash ^= (long) Math.floorMod(x, period) * 0x9E3779B97F4A7C15L;
        hash ^= (long) Math.floorMod(z, period) * 0xC2B2AE3D27D4EB4FL;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private double value3(int x, int y, int z, int period, long salt) {
        long hash = seed ^ salt;
        hash ^= (long) Math.floorMod(x, period) * 0x9E3779B97F4A7C15L;
        hash ^= (long) Math.floorMod(z, period) * 0xC2B2AE3D27D4EB4FL;
        hash ^= (long) (y + 4_096) * 0x165667B19E3779F9L;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
