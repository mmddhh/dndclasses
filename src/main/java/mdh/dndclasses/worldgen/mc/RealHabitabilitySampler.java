package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.sim.HabitabilityMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Samples the real world into a coarse habitability grid without generating chunks:
 * {@link ChunkGenerator#getBaseHeight} evaluates the noise height directly and the biome source is
 * sampled through its climate sampler.
 */
public final class RealHabitabilitySampler {

    private static final int GRID = 48;
    private static final int CELL_SIZE = 256;

    private RealHabitabilitySampler() {
    }

    public static HabitabilityMap sample(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BiomeSource biomeSource = generator.getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        int seaLevel = generator.getSeaLevel();
        int half = GRID / 2;

        double[][] habitability = new double[GRID][GRID];
        boolean[][] water = new boolean[GRID][GRID];
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                int x = (gx - half) * CELL_SIZE;
                int z = (gz - half) * CELL_SIZE;
                int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        level, level.getChunkSource().randomState());
                Holder<Biome> biome = biomeSource.getNoiseBiome(x >> 2, seaLevel >> 2, z >> 2, sampler);

                boolean ocean = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)
                        || height <= seaLevel;
                water[gz][gx] = ocean;
                if (ocean) {
                    continue;
                }

                int east = generator.getBaseHeight(x + CELL_SIZE / 4, z,
                        Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                int north = generator.getBaseHeight(x, z + CELL_SIZE / 4,
                        Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                double slope = Math.abs(height - east) + Math.abs(height - north);

                double score = 0.30D
                        + clamp((height - seaLevel) / 90.0D, 0.0D, 0.30D)
                        + biomeScore(biome)
                        - clamp(slope / 100.0D, 0.0D, 0.20D);
                if (height <= seaLevel + 6) {
                    score += 0.12D;
                }
                habitability[gz][gx] = clamp(score, 0.02D, 1.0D);
            }
        }
        return new HabitabilityMap(level.getSeed(), GRID, CELL_SIZE, habitability, water);
    }

    private static double biomeScore(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)
                || biome.is(BiomeTags.IS_JUNGLE)) {
            return 0.18D;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return 0.06D;
        }
        if (biome.is(BiomeTags.IS_HILL)) {
            return -0.04D;
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN)) {
            return -0.12D;
        }
        if (biome.is(BiomeTags.IS_BEACH)) {
            return -0.08D;
        }
        String path = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
        if (path.contains("desert") || path.contains("badlands")) {
            return -0.25D;
        }
        return 0.08D;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
