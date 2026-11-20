package mdh.dndclasses.worldgen.terrain;

/**
 * 3D terrain density using periodic gradient noise. Clean-room implementation of broad continental
 * mass, erosion and ridged mountains; no third-party source code is copied.
 */
public final class TerrainDensity {

    private static final int SEA_LEVEL = 63;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 319;

    private static final int[][] GRADIENTS_2 = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };
    private static final int[][] GRADIENTS_3 = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };

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

        double warpX = gradient2(wx, wz, 48, 0x51ED270BL) * 360.0D;
        double warpZ = gradient2(wx + 3_177, wz - 7_133, 48, 0x6A09E667L) * 360.0D;
        double warpY = gradient3(wx, y, wz, 64, 0xBB67AE85L) * 55.0D;
        double px = wx + warpX;
        double py = y + warpY;
        double pz = wz + warpZ;

        double continent = fbm3(px, py, pz, 24, 4, 0x243F6A88L);
        double erosion = fbm3(px + 1_219, py * 1.35D, pz - 2_731, 96, 4, 0x85A308D3L);
        double ridge = ridged3(px, py, pz, 220, 3, 0x13198A2EL);

        double vertical = (SEA_LEVEL - y) / 92.0D;
        double relief = (continent - 0.5D) * 2.0D * 1.95D
                + (erosion - 0.5D) * 0.48D
                + ridge * ridge * 0.95D * (0.35D + erosion * 0.65D);
        return vertical + relief - 0.10D;
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
            total += gradient3(x, y, z, currentPeriod, salt + i * 0x9E3779B9L) * amplitude;
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
            double n = gradient3(x, y, z, currentPeriod, salt + i * 0x7F4A7C15L);
            double ridge = 1.0D - Math.abs(n);
            total += ridge * ridge * amplitude;
            normal += amplitude;
            amplitude *= 0.5D;
            currentPeriod *= 2;
        }
        return normal == 0.0D ? 0.0D : total / normal;
    }

    private double gradient2(int x, int z, int period, long salt) {
        double scale = period / (double) side;
        double u = x * scale;
        double v = z * scale;
        int x0 = (int) Math.floor(u);
        int z0 = (int) Math.floor(v);
        double tx = fade(u - x0);
        double tz = fade(v - z0);
        double a = dot2(x0, z0, u - x0, v - z0, period, salt);
        double b = dot2(x0 + 1, z0, u - x0 - 1.0D, v - z0, period, salt);
        double c = dot2(x0, z0 + 1, u - x0, v - z0 - 1.0D, period, salt);
        double d = dot2(x0 + 1, z0 + 1, u - x0 - 1.0D, v - z0 - 1.0D, period, salt);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private double gradient3(double x, double y, double z, int period, long salt) {
        double scale = period / (double) side;
        double u = x * scale;
        double v = z * scale;
        double w = y * scale;
        int x0 = (int) Math.floor(u);
        int z0 = (int) Math.floor(v);
        int y0 = (int) Math.floor(w);
        double tx = fade(u - x0);
        double tz = fade(v - z0);
        double ty = fade(w - y0);

        double a = dot3(x0, y0, z0, u - x0, w - y0, v - z0, period, salt);
        double b = dot3(x0 + 1, y0, z0, u - x0 - 1.0D, w - y0, v - z0, period, salt);
        double c = dot3(x0, y0, z0 + 1, u - x0, w - y0, v - z0 - 1.0D, period, salt);
        double d = dot3(x0 + 1, y0, z0 + 1, u - x0 - 1.0D, w - y0, v - z0 - 1.0D, period, salt);
        double e = dot3(x0, y0 + 1, z0, u - x0, w - y0 - 1.0D, v - z0, period, salt);
        double f = dot3(x0 + 1, y0 + 1, z0, u - x0 - 1.0D, w - y0 - 1.0D, v - z0, period, salt);
        double g = dot3(x0, y0 + 1, z0 + 1, u - x0, w - y0 - 1.0D, v - z0 - 1.0D, period, salt);
        double h = dot3(x0 + 1, y0 + 1, z0 + 1, u - x0 - 1.0D, w - y0 - 1.0D, v - z0 - 1.0D,
                period, salt);

        double lower = lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
        double upper = lerp(lerp(e, f, tx), lerp(g, h, tx), tz);
        return lerp(lower, upper, ty) * 1.45D;
    }

    private double dot2(int x, int z, double dx, double dz, int period, long salt) {
        long hash = hash(x, 0, z, period, salt);
        int[] gradient = GRADIENTS_2[(int) (hash & 7L)];
        return gradient[0] * dx + gradient[1] * dz;
    }

    private double dot3(int x, int y, int z, double dx, double dy, double dz,
                        int period, long salt) {
        long hash = hash(x, y, z, period, salt);
        int[] gradient = GRADIENTS_3[(int) (hash & 11L)];
        return gradient[0] * dx + gradient[1] * dy + gradient[2] * dz;
    }

    private long hash(int x, int y, int z, int period, long salt) {
        long hash = seed ^ salt;
        hash ^= (long) Math.floorMod(x, period) * 0x9E3779B97F4A7C15L;
        hash ^= (long) Math.floorMod(z, period) * 0xC2B2AE3D27D4EB4FL;
        hash ^= (long) (y + 4_096) * 0x165667B19E3779F9L;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return hash;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
