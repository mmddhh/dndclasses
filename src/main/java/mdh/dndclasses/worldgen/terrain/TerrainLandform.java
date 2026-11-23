package mdh.dndclasses.worldgen.terrain;

/**
 * Landform masks layered on top of a plains matrix:
 * plains (default), hills, mountains, plateaus and basins.
 *
 * <p>The masks are low-frequency noise fields, not elevation bands, so hills cannot fill an entire
 * continent and mountains can form connected chains.
 */
public final class TerrainLandform {

    public enum Type {
        OCEAN, PLAINS, PLATEAU, BASIN, HILLS, MOUNTAINS
    }

    private final TerrainModel model;
    private final PlateModel plates;

    public TerrainLandform(TerrainModel model) {
        this.model = model;
        this.plates = new PlateModel(model.topology(), model.seed());
    }

    public Type type(int x, int z) {
        if (!plates.isLand(x, z)) {
            return Type.OCEAN;
        }
        return typeForRelief(x, z, 0);
    }

    /** Classifies the actual generated surface, not the coarse regional mask. */
    public Type typeAtSurface(int x, int z, int surface) {
        if (surface <= TerrainModel.SEA_LEVEL) {
            return Type.OCEAN;
        }
        return typeForRelief(x, z, surface - TerrainModel.SEA_LEVEL);
    }

    private Type typeForRelief(int x, int z, int relief) {
        double hill = hillMask(x, z);
        double mountain = mountainWeight(x, z);
        double plateau = plateauMask(x, z);
        double basin = basinMask(x, z);
        if (mountain > 0.45D) return Type.MOUNTAINS;
        if (plateau > 0.45D) return Type.PLATEAU;
        if (basin > 0.45D) return Type.BASIN;
        if (hill > 0.40D) return Type.HILLS;
        return Type.PLAINS;
    }

    public int targetHeight(int x, int z) {
        if (!plates.isLand(x, z)) {
            return (int) Math.round(TerrainModel.SEA_LEVEL - plates.oceanDepth(x, z));
        }
        double hill = hillMask(x, z);
        double mountain = mountainWeight(x, z);
        double plateau = plateauMask(x, z);
        double basin = basinMask(x, z);
        double hillWeight = smoothstep(0.48D, 0.62D, hill);
        double total = hillWeight + mountain + plateau + basin;
        if (total > 1.0D) {
            hillWeight /= total;
            mountain /= total;
            plateau /= total;
            basin /= total;
        }
        double plains = Math.max(0.0D, 1.0D - hillWeight - mountain - plateau - basin);

        double height = TerrainModel.SEA_LEVEL + 4.0D;
        height += hillWeight * (10.0D + fbm(x, z, 96, 0x9E3779B1L) * 18.0D);
        height += mountain * (85.0D + ridged(x, z, 28, 0x7F4A7C15L) * 165.0D);
        height += plateau * (58.0D + fbm(x, z, 128, 0x243F6A88L) * 4.0D);
        height -= basin * (10.0D + fbm(x, z, 80, 0x85A308D3L) * 8.0D);
        return (int) Math.round(Math.max(TerrainModel.SEA_LEVEL + 1,
                Math.min(TerrainModel.MAX_Y - 8, height)));
    }

    /** 0 = perfectly flat plains core, 1 = fully rugged mountains. */
    public double roughness(int x, int z) {
        double hill = hillMask(x, z);
        double mountain = mountainWeight(x, z);
        double plateau = plateauMask(x, z);
        double basin = basinMask(x, z);
        double hillWeight = smoothstep(0.48D, 0.62D, hill);
        double floor = plates.isLand(x, z) ? 0.055D : 0.12D;
        return Math.min(1.0D, floor + hillWeight * 0.55D + mountain * 0.90D
                + plateau * 0.08D + basin * 0.10D);
    }

    private double mountainWeight(int x, int z) {
        double plateBelt = smoothstep(35.0D, 130.0D, plates.uplift(x, z));
        return Math.max(mountainMask(x, z), plateBelt * 0.95D);
    }

    private double hillMask(int x, int z) {
        double n = noise(x, z, 10, 0x51ED270BL);
        double cut = smoothstep(0.72D, 0.86D, n);
        double suppress = smoothstep(0.84D, 0.96D, noise(x, z, 4, 0x6A09E667L));
        return cut * (1.0D - suppress) * 0.85D;
    }

    private double mountainMask(int x, int z) {
        double chain = 1.0D - Math.abs(noise(x, z, 14, 0xBB67AE85L) * 2.0D - 1.0D);
        double chainMask = smoothstep(0.82D, 0.94D, chain);
        double region = smoothstep(0.55D, 0.70D, noise(x, z, 4, 0x3C6EF372L));
        return chainMask * region;
    }

    private double plateauMask(int x, int z) {
        double n = noise(x, z, 3, 0xA54FF53AL);
        return smoothstep(0.74D, 0.86D, n) * (1.0D - hillMask(x, z))
                * (1.0D - mountainMask(x, z));
    }

    private double basinMask(int x, int z) {
        double n = noise(x, z, 5, 0x1F83D9ABL);
        return smoothstep(0.78D, 0.90D, n) * (1.0D - hillMask(x, z))
                * (1.0D - mountainMask(x, z)) * (1.0D - plateauMask(x, z));
    }

    private double fbm(int x, int z, int period, long salt) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double normal = 0.0D;
        int current = period;
        for (int i = 0; i < 4; i++) {
            total += (noise(x, z, current, salt + i * 0x9E3779B9L) - 0.5D) * 2.0D * amplitude;
            normal += amplitude;
            amplitude *= 0.5D;
            current *= 2;
        }
        return total / normal * 0.5D + 0.5D;
    }

    private double ridged(int x, int z, int period, long salt) {
        double n = noise(x, z, period, salt);
        double ridge = 1.0D - Math.abs(n * 2.0D - 1.0D);
        return ridge * ridge;
    }

    private double noise(int x, int z, int period, long salt) {
        int side = 62_720;
        double scale = period / (double) side;
        double u = x * scale;
        double v = z * scale;
        int x0 = (int) Math.floor(u);
        int z0 = (int) Math.floor(v);
        double tx = smooth(u - x0);
        double tz = smooth(v - z0);
        double a = value(x0, z0, period, salt);
        double b = value(x0 + 1, z0, period, salt);
        double c = value(x0, z0 + 1, period, salt);
        double d = value(x0 + 1, z0 + 1, period, salt);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private double value(int x, int z, int period, long salt) {
        long hash = salt;
        hash ^= (long) Math.floorMod(x, period) * 0x9E3779B97F4A7C15L;
        hash ^= (long) Math.floorMod(z, period) * 0xC2B2AE3D27D4EB4FL;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
