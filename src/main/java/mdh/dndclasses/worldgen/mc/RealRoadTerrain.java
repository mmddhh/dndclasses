package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.road.TerrainCost;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Real terrain cost for the road A*. Height and biome are sampled directly from the generator, so
 * the planner does not generate chunks or enqueue chunk tasks.
 */
public final class RealRoadTerrain implements TerrainCost {

    private final ServerLevel level;
    private final ChunkGenerator generator;
    private final BiomeSource biomeSource;
    private final Climate.Sampler sampler;
    private final int seaLevel;
    private final Map<Long, Integer> heightCache = new HashMap<>();
    private final Map<Long, Boolean> passableCache = new HashMap<>();

    public RealRoadTerrain(ServerLevel level) {
        this.level = level;
        this.generator = level.getChunkSource().getGenerator();
        this.biomeSource = generator.getBiomeSource();
        this.sampler = level.getChunkSource().randomState().sampler();
        this.seaLevel = generator.getSeaLevel();
    }

    @Override
    public boolean passable(int x, int z) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        Boolean cached = passableCache.get(key);
        if (cached != null) {
            return cached;
        }
        Holder<Biome> biome = biomeSource.getNoiseBiome(x >> 2, seaLevel >> 2, z >> 2, sampler);
        boolean result = height(x, z) > seaLevel
                && !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER);
        passableCache.put(key, result);
        return result;
    }

    @Override
    public int height(int x, int z) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        return heightCache.computeIfAbsent(key, ignored -> generator.getBaseHeight(x, z,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level,
                level.getChunkSource().randomState()));
    }

    @Override
    public double moveCost(int fromX, int fromZ, int toX, int toZ) {
        int from = height(fromX, fromZ);
        int to = height(toX, toZ);
        double slope = Math.abs(to - from);
        double diagonal = fromX != toX && fromZ != toZ ? 1.414D : 1.0D;
        return diagonal * (1.0D + slope / 6.0D);
    }
}
