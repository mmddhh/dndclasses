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
                int surface = density.surfaceHeight(worldX, worldZ);
                height[index] = (short) surface;
                water[index] = surface < SEA_LEVEL;
                TerrainModel.Biome kind = water[index]
                        ? TerrainModel.Biome.OCEAN
                        : climate.biome(worldX, worldZ, surface);
                biome[index] = (byte) kind.ordinal();
                temperature[index] = (float) climate.temperature(worldX, worldZ);
                humidity[index] = (float) climate.humidity(worldX, worldZ);
                habitability[index] = water[index] ? 0.0F
                        : (float) Math.max(0.0D, 1.0D - Math.abs(surface - 92.0D) / 180.0D);
            }
        }
        return new TerrainGrid(topology, seed, height, biome, temperature, humidity,
                water, new boolean[size * size], new boolean[size * size], new int[size * size],
                List.of(), habitability);
    }
}
