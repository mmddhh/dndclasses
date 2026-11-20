package mdh.dndclasses.worldgen.mc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mdh.dndclasses.worldgen.terrain.TerrainModel;
import mdh.dndclasses.worldgen.terrain.WorldTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * First in-game terrain adapter. It deliberately uses a finite vanilla world (no SpatialLoop yet)
 * so the terrain model can be inspected with a map mod before the loop provider is fixed.
 */
public final class TerrainChunkGenerator extends ChunkGenerator {

    public static final Codec<TerrainChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    Codec.LONG.optionalFieldOf("seed", 12345L).forGetter(generator -> generator.seed)
            ).apply(instance, TerrainChunkGenerator::new));

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;
    private static final int SEA_LEVEL = 63;
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();

    private final long seed;
    private final TerrainModel model;

    public TerrainChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.seed = seed;
        this.model = new TerrainModel(WorldTopology.standard(), seed);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level,
                                     ChunkAccess chunk, StructureManager structureManager) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                       RandomState randomState,
                                                       StructureManager structureManager,
                                                       ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            fill(chunk);
            return chunk;
        }, executor);
    }

    private void fill(ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = minX + localX;
                int z = minZ + localZ;
                int height = Math.max(MIN_Y + 5, Math.min(MIN_Y + HEIGHT - 10, model.height(x, z)));
                TerrainModel.Biome biome = model.biome(x, z);
                for (int y = MIN_Y; y <= height; y++) {
                    BlockState state;
                    if (y == MIN_Y) {
                        state = BEDROCK;
                    } else if (y < 0) {
                        state = DEEPSLATE;
                    } else if (y < height - 3) {
                        state = STONE;
                    } else {
                        state = surfaceState(biome, y, height);
                    }
                    chunk.setBlockState(new BlockPos(x, y, z), state, false);
                }
                if (height < SEA_LEVEL) {
                    for (int y = height + 1; y <= SEA_LEVEL; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), WATER, false);
                    }
                }
            }
        }
    }

    private static BlockState surfaceState(TerrainModel.Biome biome, int y, int height) {
        if (biome == TerrainModel.Biome.DESERT || biome == TerrainModel.Biome.BEACH) {
            return y == height ? SAND : SAND;
        }
        if (biome == TerrainModel.Biome.TUNDRA) {
            return y == height ? SNOW : STONE;
        }
        if (biome == TerrainModel.Biome.MOUNTAINS) {
            return y == height ? STONE : STONE;
        }
        if (biome == TerrainModel.Biome.OCEAN) {
            return y == height ? GRAVEL : STONE;
        }
        return y == height ? GRASS : DIRT;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor level, RandomState randomState) {
        return Math.max(MIN_Y + 5, Math.min(MIN_Y + HEIGHT - 10, model.height(x, z)));
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
                                     RandomState randomState) {
        BlockState[] states = new BlockState[HEIGHT];
        int height = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        for (int index = 0; index < HEIGHT; index++) {
            int y = MIN_Y + index;
            states[index] = y > height ? Blocks.AIR.defaultBlockState() : STONE;
        }
        return new NoiseColumn(MIN_Y, states);
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> lines, RandomState randomState,
                                   BlockPos pos) {
        lines.add("dndclasses terrain seed=" + seed);
    }
}
