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
    private final TerrainModel continent;

    public TerrainDensity(WorldTopology topology, long seed) {
        this.topology = topology;
        this.seed = seed;
        this.side = topology.sideBlocks();
        this.continent = new TerrainModel(topology, seed);
    }

    /** Positive is solid, negative is air or water. */
    public double density(int x, int y, int z) {
        int wx = topology.wrap(x);
        int wz = topology.wrap(z);

        // 2D continental mask only biases land vs. ocean; it is not used as the surface height.
        double mask = clamp((continent.height(x, z) - SEA_LEVEL) / 95.0D, -1.7D, 1.7D);

        double warpX = gradient2(wx, wz, 48, 0x51ED270BL) * 260.0D;
        double warpZ = gradient2(wx + 3_177, wz - 7_133, 48, 0x6A09E667L) * 260.0D;
        double warpY = gradient3(wx, y, wz, 64, 0xBB67AE85L) * 36.0D;
        double px = wx + warpX;
        double py = y + warpY;
        double pz = wz + warpZ;

        double broad = (fbm3(px, py, pz, 24, 4, 0x243F6A88L) - 0.5D) * 2.0D;
        double detail = (fbm3(px + 1_219, py * 1.35D, pz - 2_731, 96, 4, 0x85A308D3L) - 0.5D) * 2.0D;
        double ridge = ridged3(px, py, pz, 220, 3, 0x13198A2EL);

        // The mountain field gets its own warp, otherwise high peaks fall back to closed rings.
        double mountainWarpX = gradient2(px, pz, 32, 0x3C6EF372L) * 150.0D;
        double mountainWarpZ = gradient2(px + 4_091, pz - 2_237, 32, 0xA54FF53AL) * 150.0D;
        double mx = px + mountainWarpX;
        double mz = pz + mountainWarpZ;
        double belt = ridge2(mx, mz, 18, 0x9E3779B1L);
        double mountain = smoothstep(0.38D, 0.72D, belt) * (0.35D + ridge * 0.65D);

        // A second, finer ridge network is subtracted to cut V-shaped tributary valleys.
        double tributary = ridge2(mx + 2_113, mz - 4_127, 64, 0x7F4A7C15L);
        double valley = smoothstep(0.78D, 0.97D, tributary);

        double vertical = (SEA_LEVEL - y) / 96.0D;
        return vertical
                + mask * 1.35D
                + broad * 0.85D
                + detail * 0.30D
                + ridge * ridge * 0.55D * (0.45D + 0.55D * (detail * 0.5D + 0.5D))
                + mountain * 0.55D
                - valley * 0.42D;
    }

    private double ridge2(double x, double z, int period, long salt) {
        double n = Math.max(-1.0D, Math.min(1.0D, gradient2(x, z, period, salt)));
        return 1.0D - Math.abs(n);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
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
        return gradient2((double) x, (double) z, period, salt);
    }

    private double gradient2(double x, double z, int period, long salt) {
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
