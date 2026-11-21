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
        return new TerrainGrid(topology, seed, height, biome, temperature, humidity,
                water, new boolean[size * size], new boolean[size * size], new int[size * size],
                List.of(), habitability);
    }
}
