package mdh.dndclasses.worldgen.terrain;

/** Builds the coarse 245x245 habitability grid from {@link TerrainModel} without any Minecraft API. */
public final class HabitabilitySampler {

    private HabitabilitySampler() {
    }

    public static TerrainGrid sample(WorldTopology topology, long seed) {
        TerrainModel model = new TerrainModel(topology, seed);
        int size = topology.gridSize();
        short[] height = new short[size * size];
        byte[] biome = new byte[size * size];
        float[] temperature = new float[size * size];
        float[] humidity = new float[size * size];
        boolean[] water = new boolean[size * size];
        float[] habitability = new float[size * size];

        for (int gz = 0; gz < size; gz++) {
            for (int gx = 0; gx < size; gx++) {
                int x = topology.cellCenter(gx);
                int z = topology.cellCenter(gz);
                int index = gz * size + gx;
                int h = model.height(x, z);
                TerrainModel.Biome b = model.biome(x, z);
                height[index] = (short) h;
                biome[index] = (byte) b.ordinal();
                temperature[index] = (float) model.temperature(x, z);
                humidity[index] = (float) model.humidity(x, z);
                water[index] = h <= TerrainModel.SEA_LEVEL;
            }
        }

        for (int gz = 0; gz < size; gz++) {
            for (int gx = 0; gx < size; gx++) {
                int index = gz * size + gx;
                if (water[index]) {
                    continue;
                }
                int h = height[index];
                int east = height[gz * size + Math.floorMod(gx + 1, size)];
                int north = height[Math.floorMod(gz + 1, size) * size + gx];
                double slope = Math.abs(h - east) + Math.abs(h - north);

                boolean coast = false;
                for (int dx = -2; dx <= 2 && !coast; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (water[Math.floorMod(gz + dz, size) * size
                                + Math.floorMod(gx + dx, size)]) {
                            coast = true;
                            break;
                        }
                    }
                }

                double temperatureComfort = 1.0D
                        - Math.abs(temperature[index] - 18.0D) / 30.0D;
                temperatureComfort = clamp(temperatureComfort, 0.0D, 1.0D);
                double flatness = 1.0D - clamp(slope / 12.0D, 0.0D, 1.0D);
                double coastScore = coast ? 1.0D : 0.0D;
                double biomeScore = TerrainModel.biomeScore(TerrainModel.Biome.values()
                        [biome[index] & 0xff]);

                double score = 0.35D * temperatureComfort
                        + 0.30D * flatness
                        + 0.20D * coastScore
                        + 0.15D * Math.max(0.0D, biomeScore);
                habitability[index] = (float) clamp(score, 0.0D, 1.0D);
            }
        }
        return new TerrainGrid(topology, seed, height, biome, temperature, humidity, water,
                habitability);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
