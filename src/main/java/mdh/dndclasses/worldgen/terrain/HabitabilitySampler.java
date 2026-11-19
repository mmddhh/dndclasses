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
        int[] waterDistance = new int[size * size];
        java.util.Arrays.fill(waterDistance, -1);

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

        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        RiverNetwork.Result rivers = RiverNetwork.compute(topology, height, water);
        for (int i = 0; i < water.length; i++) {
            if (water[i] || rivers.river()[i]) {
                waterDistance[i] = 0;
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int current = queue.poll();
            int gx = current % size;
            int gz = current / size;
            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = Math.floorMod(gx + dir[0], size);
                int nz = Math.floorMod(gz + dir[1], size);
                int next = nz * size + nx;
                if (waterDistance[next] == -1) {
                    waterDistance[next] = waterDistance[current] + 1;
                    queue.add(next);
                }
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

                double temperatureComfort = 1.0D
                        - clamp(Math.abs(temperature[index] - 18.0D) / 16.0D, 0.0D, 1.0D);
                double climatePenalty = temperature[index] < 0.0D
                        || temperature[index] > 35.0D ? 0.35D : 1.0D;
                double flatness = 1.0D - clamp(slope / 12.0D, 0.0D, 1.0D);
                double waterScore = waterDistance[index] <= 0 ? 0.0D
                        : 1.0D - clamp((waterDistance[index] - 1) / 10.0D, 0.0D, 1.0D);
                double biomeScore = TerrainModel.biomeScore(TerrainModel.Biome.values()
                        [biome[index] & 0xff]);

                double score = (0.45D * temperatureComfort
                        + 0.30D * waterScore
                        + 0.15D * flatness
                        + 0.10D * Math.max(0.0D, biomeScore)) * climatePenalty;
                habitability[index] = (float) clamp(score, 0.0D, 1.0D);
            }
        }

        // Smooth only land cells; water stays at habitability 0.
        float[] smoothed = habitability.clone();
        for (int gz = 0; gz < size; gz++) {
            for (int gx = 0; gx < size; gx++) {
                int index = gz * size + gx;
                if (water[index]) {
                    continue;
                }
                double sum = 0.0D;
                int count = 0;
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = Math.floorMod(gx + dx, size);
                        int nz = Math.floorMod(gz + dz, size);
                        int other = nz * size + nx;
                        if (!water[other]) {
                            sum += habitability[other];
                            count++;
                        }
                    }
                }
                smoothed[index] = (float) (sum / count);
            }
        }
        return new TerrainGrid(topology, seed, height, biome, temperature, humidity, water,
                rivers.river(), rivers.flow(), smoothed);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
