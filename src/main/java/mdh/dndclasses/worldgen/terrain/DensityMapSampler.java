package mdh.dndclasses.worldgen.terrain;

import java.util.List;

/** Samples the same 3D density field used in game into the PNG terrain grid. */
public final class DensityMapSampler {

    private static final int SEA_LEVEL = 63;

    private DensityMapSampler() {
    }

    public static TerrainGrid sample(WorldTopology topology, long seed) {
        int size = topology.gridSize();
        TerrainModel climate = new TerrainModel(topology, seed);
        TerrainDensity density = new TerrainDensity(topology, seed);
        short[] height = new short[size * size];
        byte[] biome = new byte[size * size];
        float[] temperature = new float[size * size];
        float[] humidity = new float[size * size];
        boolean[] water = new boolean[size * size];
        float[] habitability = new float[size * size];

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                int worldX = topology.cellCenter(x);
                int worldZ = topology.cellCenter(z);
                height[index] = (short) density.surfaceHeight(worldX, worldZ);
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            short[] copy = height.clone();
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int total = 0;
                    int count = 0;
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int sx = Math.max(0, Math.min(size - 1, x + dx));
                            int sz = Math.max(0, Math.min(size - 1, z + dz));
                            total += copy[sz * size + sx];
                            count++;
                        }
                    }
                    height[z * size + x] = (short) Math.round(total / (float) count);
                }
            }
        }

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                int worldX = topology.cellCenter(x);
                int worldZ = topology.cellCenter(z);
                int surface = height[index];
                water[index] = surface < SEA_LEVEL;
                TerrainModel.Biome kind = water[index]
                        ? TerrainModel.Biome.OCEAN
                        : climate.biome(worldX, worldZ, surface);
                biome[index] = (byte) kind.ordinal();
                temperature[index] = (float) climate.temperature(worldX, worldZ, surface);
                humidity[index] = (float) climate.humidity(worldX, worldZ, surface);
                habitability[index] = water[index] ? 0.0F
                        : (float) Math.max(0.0D, 1.0D - Math.abs(surface - 92.0D) / 180.0D);
            }
        }
        D8Hydrology.Result hydrology = D8Hydrology.compute(topology, height, water);
        for (int i = 0; i < water.length; i++) {
            if (hydrology.lake()[i]) {
                water[i] = true;
                biome[i] = (byte) TerrainModel.Biome.OCEAN.ordinal();
            }
        }
        List<RiverPath> detailedPaths = detailedPaths(topology, size, height, water);
        return new TerrainGrid(topology, seed, height, biome, temperature, humidity,
                water, hydrology.lake(), hydrology.river(), hydrology.flow(),
                detailedPaths, habitability);
    }

    /** Four times the map resolution: 64 blocks per hydrology cell. */
    private static List<RiverPath> detailedPaths(WorldTopology topology, int baseSize,
                                                 short[] baseHeight, boolean[] baseWater) {
        int factor = 4;
        int size = baseSize * factor;
        short[] height = new short[size * size];
        boolean[] water = new boolean[size * size];
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double gx = x / (double) factor;
                double gz = z / (double) factor;
                int x0 = Math.min(baseSize - 1, (int) Math.floor(gx));
                int z0 = Math.min(baseSize - 1, (int) Math.floor(gz));
                int x1 = Math.min(baseSize - 1, x0 + 1);
                int z1 = Math.min(baseSize - 1, z0 + 1);
                double tx = gx - x0;
                double tz = gz - z0;
                double a = baseHeight[z0 * baseSize + x0];
                double b = baseHeight[z0 * baseSize + x1];
                double c = baseHeight[z1 * baseSize + x0];
                double d = baseHeight[z1 * baseSize + x1];
                double h = (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * tz;
                int index = z * size + x;
                height[index] = (short) Math.round(h);
                water[index] = h < SEA_LEVEL;
            }
        }
        D8Hydrology.Result result = D8Hydrology.compute(size, height, water);
        List<RiverPath> paths = new java.util.ArrayList<>();
        for (RiverPath path : result.paths()) {
            java.util.List<RiverPath.Node> nodes = new java.util.ArrayList<>();
            RiverPath.Node previous = null;
            for (RiverPath.Node node : path.points()) {
                RiverPath.Node mapped = new RiverPath.Node(node.x() / factor, node.z() / factor);
                if (previous == null || mapped.x() != previous.x() || mapped.z() != previous.z()) {
                    nodes.add(mapped);
                    previous = mapped;
                }
            }
            if (nodes.size() >= 2) {
                paths.add(new RiverPath(java.util.List.copyOf(nodes), path.width()));
            }
        }
        return paths;
    }
}
