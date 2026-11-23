package mdh.dndclasses.worldgen.terrain;

import java.util.Random;

/**
 * Deterministic kinematic plate model. Plates are Voronoi cells on the torus; each cell is
 * continental or oceanic, and the boundary type contributes uplift, rift or trench depth.
 */
public final class PlateModel {

    private static final int GRID = 245;
    private static final int CELL = 256;
    private static final int SEA = TerrainModel.SEA_LEVEL;
    private static final int PLATE_COUNT = 16;

    private record Plate(int x, int z, boolean continental, int motionX, int motionZ) {
    }

    private final WorldTopology topology;
    private final long seed;
    private final Plate[] plates;
    private final float[] base;
    private final float[] uplift;
    private final float[] land;

    public PlateModel(WorldTopology topology, long seed) {
        this.topology = topology;
        this.seed = seed;
        this.plates = new Plate[PLATE_COUNT];
        Random random = new Random(seed ^ 0x5DEECE66DL);
        for (int i = 0; i < PLATE_COUNT; i++) {
            plates[i] = new Plate(
                    random.nextInt(topology.sideBlocks()),
                    random.nextInt(topology.sideBlocks()),
                    random.nextDouble() < 0.56D,
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1);
        }
        this.base = new float[GRID * GRID];
        this.uplift = new float[GRID * GRID];
        this.land = new float[GRID * GRID];
        build();
    }

    public boolean isLand(int x, int z) {
        double coast = (noise(x, z, 96, 0x3C6EF372L) - 0.5D) * 0.22D;
        return sample(land, x, z) + coast > 0.5D;
    }

    public double oceanDepth(int x, int z) {
        return Math.max(20.0D, SEA - sample(base, x, z));
    }

    public double uplift(int x, int z) {
        return sample(uplift, x, z);
    }

    private void build() {
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                int x = topology.wrap(gx * CELL + CELL / 2);
                int z = topology.wrap(gz * CELL + CELL / 2);
                int first = nearest(x, z);
                int second = secondNearest(x, z, first);
                Plate a = plates[first];
                Plate b = plates[second];
                int boundary = boundaryDistance(x, z, first, second);
                double influence = Math.max(0.0D, 1.0D - boundary / 760.0D);
                influence = influence * influence;

                boolean continental = a.continental;
                double h = continental ? SEA + 22.0D : SEA - 80.0D;
                double up = 0.0D;
                if (influence > 0.0D) {
                    if (a.continental && b.continental) {
                        up = 150.0D * influence;
                    } else if (a.continental || b.continental) {
                        h = SEA - 115.0D * influence;
                    } else {
                        h = SEA - 55.0D * influence;
                    }
                }
                int index = gz * GRID + gx;
                base[index] = (float) h;
                uplift[index] = (float) up;
                land[index] = continental ? 1.0F : 0.0F;
            }
        }
    }

    private int nearest(int x, int z) {
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < plates.length; i++) {
            int distance = topology.distance(x, plates[i].x)
                    + topology.distance(z, plates[i].z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private int secondNearest(int x, int z, int exclude) {
        int best = exclude == 0 ? 1 : 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < plates.length; i++) {
            if (i == exclude) continue;
            int distance = topology.distance(x, plates[i].x)
                    + topology.distance(z, plates[i].z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private int boundaryDistance(int x, int z, int a, int b) {
        int da = topology.distance(x, plates[a].x) + topology.distance(z, plates[a].z);
        int db = topology.distance(x, plates[b].x) + topology.distance(z, plates[b].z);
        return Math.abs(da - db);
    }

    private double sample(float[] values, int x, int z) {
        int warpedX = topology.wrap(x + (int) Math.round(
                (noise(x, z, 48, 0x51ED270BL) - 0.5D) * 260.0D));
        int warpedZ = topology.wrap(z + (int) Math.round(
                (noise(x + 7_133, z - 3_177, 48, 0x6A09E667L) - 0.5D) * 260.0D));
        double gx = warpedX / (double) CELL;
        double gz = warpedZ / (double) CELL;
        int x0 = (int) Math.floor(gx);
        int z0 = (int) Math.floor(gz);
        int x1 = Math.floorMod(x0 + 1, GRID);
        int z1 = Math.floorMod(z0 + 1, GRID);
        double tx = gx - x0;
        double tz = gz - z0;
        double a = values[Math.floorMod(z0, GRID) * GRID + Math.floorMod(x0, GRID)];
        double b = values[Math.floorMod(z0, GRID) * GRID + x1];
        double c = values[z1 * GRID + Math.floorMod(x0, GRID)];
        double d = values[z1 * GRID + x1];
        return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * tz;
    }

    private double noise(int x, int z, int period, long salt) {
        double scale = period / (double) topology.sideBlocks();
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
        long hash = seed ^ salt;
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

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
