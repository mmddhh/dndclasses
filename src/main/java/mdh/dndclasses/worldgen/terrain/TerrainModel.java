package mdh.dndclasses.worldgen.terrain;

import java.util.Random;

/**
 * Pure-Java fantasy terrain model. Every noise lattice is periodic in both axes, so the same
 * functions can later be used by a wrapped chunk generator and by the coarse habitability sampler.
 */
public final class TerrainModel {

    public static final int SEA_LEVEL = 63;
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 320;

    private final WorldTopology topology;
    private final long seed;
    private final TileableNoise continent;
    private final TileableNoise continentDetail;
    private final TileableNoise detail;
    private final TileableNoise ridgeNoise;
    private final TileableNoise warpNoise;
    private final TileableNoise coastNoise;
    private final TileableNoise temperatureNoise;
    private final TileableNoise humidityNoise;

    public TerrainModel(WorldTopology topology, long seed) {
        this.topology = topology;
        this.seed = seed;
        this.continent = new TileableNoise(seed ^ 0x1111L, topology, 7);
        this.continentDetail = new TileableNoise(seed ^ 0x7777L, topology, 19);
        this.detail = new TileableNoise(seed ^ 0x2222L, topology, 17);
        this.ridgeNoise = new TileableNoise(seed ^ 0x3333L, topology, 37);
        this.warpNoise = new TileableNoise(seed ^ 0x6666L, topology, 5);
        this.coastNoise = new TileableNoise(seed ^ 0x8888L, topology, 53);
        this.temperatureNoise = new TileableNoise(seed ^ 0x4444L, topology, 11);
        this.humidityNoise = new TileableNoise(seed ^ 0x5555L, topology, 17);
    }

    public WorldTopology topology() {
        return topology;
    }

    public long seed() {
        return seed;
    }

    /** Approximate surface height in blocks, including ocean depth and mountain ridges. */
    public int height(int x, int z) {
        int wx = topology.wrap(x);
        int wz = topology.wrap(z);
        double warpX = (warpNoise.sample(wx, wz) - 0.5D) * 900.0D
                + (coastNoise.sample(wx, wz) - 0.5D) * 900.0D;
        double warpZ = (warpNoise.sample(wx + 3_731, wz - 1_919) - 0.5D) * 900.0D
                + (coastNoise.sample(wx - 2_111, wz + 4_013) - 0.5D) * 900.0D;
        int sampledX = topology.wrap((int) Math.round(wx + warpX));
        int sampledZ = topology.wrap((int) Math.round(wz + warpZ));

        double broad = continent.sample(sampledX, sampledZ);
        broad += (continentDetail.sample(sampledX, sampledZ) - 0.5D) * 0.22D;
        double fine = detail.sample(wx, wz);
        double ridge = 1.0D - Math.abs(ridgeNoise.sample(sampledX, sampledZ) * 2.0D - 1.0D);

        double seaThreshold = 0.50D;
        if (broad < seaThreshold) {
            // Ocean floor: shallow shelf near the coast, then a deeper basin.
            double depth = (seaThreshold - broad) * 240.0D
                    + Math.max(0.0D, 0.55D - broad) * 80.0D
                    + (fine - 0.5D) * 18.0D;
            return (int) Math.max(MIN_Y + 4, Math.round(SEA_LEVEL - depth));
        }

        double base = SEA_LEVEL + (broad - seaThreshold) * 165.0D;
        double roughness = (fine - 0.5D) * 34.0D;
        double coastRoughness = 0.0D;
        if (broad < seaThreshold + 0.08D) {
            coastRoughness = (coastNoise.sample(wx, wz) - 0.5D) * 22.0D;
        }
        double mountainMask = smoothstep(0.55D, 0.78D, broad);
        double mountain = Math.pow(Math.max(0.0D, ridge), 2.2D) * 210.0D * mountainMask;
        double value = base + roughness + coastRoughness + mountain;
        return (int) Math.max(SEA_LEVEL + 1, Math.min(MAX_Y - 8, Math.round(value)));
    }

    public boolean water(int x, int z) {
        return height(x, z) <= SEA_LEVEL;
    }

    /**
     * Folded latitude in centered coordinates: 0 at (0,0) south pole and at the z=±half seam
     * north pole, 1 at the two symmetric equators z=±half/2.
     */
    public double latitude(int z) {
        double centered = topology.center(z) / (topology.sideBlocks() / 2.0D);
        return Math.abs(Math.sin(Math.PI * centered));
    }

    public double temperature(int x, int z) {
        int height = height(x, z);
        double latitude = latitude(z);
        double altitude = Math.max(0, height - SEA_LEVEL) * 0.006D;
        double noise = (temperatureNoise.sample(topology.wrap(x), topology.wrap(z)) - 0.5D) * 6.0D;
        return -8.0D + 32.0D * latitude - altitude + noise;
    }

    public double humidity(int x, int z) {
        int height = height(x, z);
        double noise = humidityNoise.sample(topology.wrap(x), topology.wrap(z));
        double coast = 0.0D;
        for (int[] dir : new int[][]{{64, 0}, {-64, 0}, {0, 64}, {0, -64}}) {
            if (height(x + dir[0], z + dir[1]) <= SEA_LEVEL) {
                coast = 0.18D;
                break;
            }
        }
        double altitudeDry = Math.max(0, height - SEA_LEVEL) * 0.0015D;
        return clamp(noise + coast - altitudeDry, 0.0D, 1.0D);
    }

    public Biome biome(int x, int z) {
        int height = height(x, z);
        if (height <= SEA_LEVEL) {
            return Biome.OCEAN;
        }
        if (height <= SEA_LEVEL + 2) {
            return Biome.BEACH;
        }
        double temperature = temperature(x, z);
        double humidity = humidity(x, z);
        if (height > 180) {
            return Biome.MOUNTAINS;
        }
        if (height > 120) {
            return Biome.HILLS;
        }
        if (temperature < -5.0D) {
            return Biome.TUNDRA;
        }
        if (temperature > 26.0D && humidity < 0.30D) {
            return Biome.DESERT;
        }
        if (temperature > 22.0D && humidity > 0.65D) {
            return Biome.JUNGLE;
        }
        if (temperature > 18.0D && humidity < 0.40D) {
            return Biome.SAVANNA;
        }
        if (temperature < 8.0D) {
            return Biome.TAIGA;
        }
        if (humidity > 0.72D && height < SEA_LEVEL + 12) {
            return Biome.SWAMP;
        }
        return humidity > 0.48D ? Biome.FOREST : Biome.PLAINS;
    }

    /** Biome-specific contribution to habitability, roughly in [-0.3, 0.25]. */
    public static double biomeScore(Biome biome) {
        return switch (biome) {
            case FOREST -> 0.20D;
            case PLAINS -> 0.15D;
            case SAVANNA -> 0.10D;
            case JUNGLE -> 0.05D;
            case TAIGA -> 0.02D;
            case SWAMP -> -0.05D;
            case BEACH -> -0.05D;
            case HILLS -> 0.00D;
            case TUNDRA -> -0.15D;
            case MOUNTAINS -> -0.18D;
            case DESERT -> -0.25D;
            case OCEAN -> -1.00D;
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public enum Biome {
        OCEAN,
        BEACH,
        TUNDRA,
        TAIGA,
        FOREST,
        PLAINS,
        SAVANNA,
        JUNGLE,
        SWAMP,
        DESERT,
        HILLS,
        MOUNTAINS
    }

    /**
     * Periodic value noise. The lattice wraps every {@code period} cells; because the sample
     * coordinate is scaled by {@code period / sideBlocks}, the field is seamless in both axes.
     */
    private static final class TileableNoise {
        private final double[] lattice;
        private final int period;
        private final double scale;

        private TileableNoise(long seed, WorldTopology topology, int period) {
            this.period = period;
            this.scale = period / (double) topology.sideBlocks();
            this.lattice = new double[period * period];
            Random random = new Random(seed);
            for (int i = 0; i < lattice.length; i++) {
                lattice[i] = random.nextDouble();
            }
        }

        private double sample(int x, int z) {
            double u = x * scale;
            double v = z * scale;
            int x0 = (int) Math.floor(u);
            int z0 = (int) Math.floor(v);
            double tx = smooth(u - x0);
            double tz = smooth(v - z0);
            double a = value(x0, z0);
            double b = value(x0 + 1, z0);
            double c = value(x0, z0 + 1);
            double d = value(x0 + 1, z0 + 1);
            return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
        }

        private double value(int x, int z) {
            return lattice[Math.floorMod(z, period) * period + Math.floorMod(x, period)];
        }

        private static double smooth(double t) {
            return t * t * (3.0D - 2.0D * t);
        }

        private static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }
}
