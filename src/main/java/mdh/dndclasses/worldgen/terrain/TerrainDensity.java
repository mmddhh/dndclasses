package mdh.dndclasses.worldgen.terrain;

/**
 * Small 3D density field used by the in-game generator. It is a clean-room implementation based on
 * the general idea of density fields and erosion; no third-party source is copied.
 */
public final class TerrainDensity {

    private final TerrainModel model;
    private final int side;
    private final long seed;

    public TerrainDensity(WorldTopology topology, TerrainModel model, long seed) {
        this.model = model;
        this.side = topology.sideBlocks();
        this.seed = seed;
    }

    /** Positive = solid, negative = air/water. */
    public double density(int x, int y, int z) {
        int surface = model.height(x, z);
        double vertical = (surface - y) / 7.0D;
        double broad = (noise(x, y, z, 256) - 0.5D) * 1.4D;
        double detail = (noise(x, y, z, 1024) - 0.5D) * 0.6D;
        return vertical + broad + detail;
    }

    private double noise(int x, int y, int z, int period) {
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
        double a = value(x0, y0, z0, period);
        double b = value(x0 + 1, y0, z0, period);
        double c = value(x0, y0, z0 + 1, period);
        double d = value(x0 + 1, y0, z0 + 1, period);
        double e = value(x0, y0 + 1, z0, period);
        double f = value(x0 + 1, y0 + 1, z0, period);
        double g = value(x0, y0 + 1, z0 + 1, period);
        double h = value(x0 + 1, y0 + 1, z0 + 1, period);
        double lower = lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
        double upper = lerp(lerp(e, f, tx), lerp(g, h, tx), tz);
        return lerp(lower, upper, ty);
    }

    private double value(int x, int y, int z, int period) {
        long hash = seed;
        hash ^= (long) Math.floorMod(x, period) * 0x9E3779B97F4A7C15L;
        hash ^= (long) Math.floorMod(z, period) * 0xC2B2AE3D27D4EB4FL;
        hash ^= (long) Math.floorMod(y, period) * 0x165667B19E3779F9L;
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
