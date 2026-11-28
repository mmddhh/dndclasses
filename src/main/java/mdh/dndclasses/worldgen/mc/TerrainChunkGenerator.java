package mdh.dndclasses.worldgen.mc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mdh.dndclasses.worldgen.terrain.TerrainColumns;
import mdh.dndclasses.worldgen.terrain.TerrainBiomeSource;
import mdh.dndclasses.worldgen.terrain.WorldTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.SnowLayerBlock;
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
                    Codec.LONG.optionalFieldOf("seed", 12345L).forGetter(generator -> generator.columns.seed())
            ).apply(instance, TerrainChunkGenerator::new));

    private static final int MIN_Y = TerrainColumns.MIN_Y;
    private static final int HEIGHT = TerrainColumns.HEIGHT;
    private static final int SEA_LEVEL = TerrainColumns.SEA_LEVEL;
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState ICE = Blocks.ICE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private volatile TerrainColumns columns;

    public TerrainChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        setWorldSeed(seed);
    }

    public void setWorldSeed(long worldSeed) {
        if (biomeSource instanceof TerrainBiomeSource terrainBiomes) {
            columns = terrainBiomes.columnsForSeed(worldSeed);
        } else if (columns == null || columns.seed() != worldSeed) {
            columns = new TerrainColumns(worldSeed);
        }
    }

    public boolean usesSnapshot() {
        return biomeSource instanceof TerrainBiomeSource terrainBiomes && terrainBiomes.usesSnapshot();
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
        super.applyBiomeDecoration(level, chunk, structureManager);
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
        TerrainColumns sampler = columns;
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int bottom = Math.max(MIN_Y, chunk.getMinBuildHeight());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = minZ; z < minZ + 16; z++) {
            for (int x = minX; x < minX + 16; x++) {
                TerrainColumns.Column column = sampler.sample(x, z);
                BlockState surface = groundState(column, x, z);
                int top = Math.min(top(column), chunk.getMaxBuildHeight() - 1);
                for (int y = bottom; y <= top; y++) {
                    chunk.setBlockState(pos.set(x, y, z), blockAt(sampler, column, surface, x, y, z), false);
                }
            }
        }
    }

    private static int top(TerrainColumns.Column column) {
        return Math.max(column.ground(), column.water());
    }

    private static BlockState blockAt(TerrainColumns sampler, TerrainColumns.Column column,
                                      BlockState surface, int x, int y, int z) {
        if (y == MIN_Y) {
            return BEDROCK;
        }
        if (y < column.ground()) {
            if (sampler.isCave(x, y, z, column)) return CAVE_AIR;
            if (surface == GRASS && y >= column.ground() - 3) return DIRT;
            return y < 0 ? DEEPSLATE : STONE;
        }
        if (y == column.ground()) {
            return surface;
        }
        if (y <= column.water()) {
            return y == column.water() && column.waterTemperature() <= 0.0D ? ICE : WATER;
        }
        return AIR;
    }

    private static final int SNOW_NOISE_PERIOD = 653;

    private static double snowJitter(int x, int z) {
        double scale = SNOW_NOISE_PERIOD / (double) WorldTopology.DEFAULT_SIDE;
        double u = Math.floorMod(x, WorldTopology.DEFAULT_SIDE) * scale;
        double v = Math.floorMod(z, WorldTopology.DEFAULT_SIDE) * scale;
        int x0 = (int) Math.floor(u);
        int z0 = (int) Math.floor(v);
        double tx = u - x0;
        double tz = v - z0;
        double a = snowNoise(x0, z0);
        double b = snowNoise(x0 + 1, z0);
        double c = snowNoise(x0, z0 + 1);
        double d = snowNoise(x0 + 1, z0 + 1);
        double lower = a + (b - a) * tx;
        double upper = c + (d - c) * tx;
        return (lower + (upper - lower) * tz) * 2.0D - 1.0D;
    }

    private static double snowNoise(int x, int z) {
        long h = Math.floorMod(x, SNOW_NOISE_PERIOD) * 0x9E3779B97F4A7C15L
                ^ Math.floorMod(z, SNOW_NOISE_PERIOD) * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    private static BlockState groundState(TerrainColumns.Column column, int x, int z) {
        if (column.river() || column.ground() < SEA_LEVEL - 2) {
            return GRAVEL;
        }
        if (column.ground() < SEA_LEVEL) {
            return SAND;
        }
        double temperature = column.groundTemperature() + snowJitter(x, z) * 3.5D;
        if (temperature <= 0.0D) {
            return SNOW;
        }
        if (temperature <= 3.0D) {
            int layers = Math.max(1, Math.min(3, (int) Math.ceil(3.0D - temperature)));
            return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        }
        return switch (column.biome()) {
            case DESERT, BEACH -> SAND;
            case TUNDRA -> SNOW;
            case MOUNTAINS -> STONE;
            default -> GRASS;
        };
    }

    public boolean isDryLand(int x, int z) {
        TerrainColumns.Column column = columns.sample(x, z);
        return column.ground() >= SEA_LEVEL && column.water() < column.ground();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor level, RandomState randomState) {
        TerrainColumns.Column column = columns.sample(x, z);
        BlockState surface = groundState(column, x, z);
        int bottom = Math.max(MIN_Y, level.getMinBuildHeight());
        int top = Math.min(top(column), level.getMaxBuildHeight() - 1);
        for (int y = top; y >= bottom; y--) {
            if (type.isOpaque().test(blockAt(columns, column, surface, x, y, z))) {
                return y + 1;
            }
        }
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
                                     RandomState randomState) {
        TerrainColumns.Column column = columns.sample(x, z);
        BlockState surface = groundState(column, x, z);
        int bottom = Math.max(MIN_Y, level.getMinBuildHeight());
        int limit = Math.min(MIN_Y + HEIGHT, level.getMaxBuildHeight());
        BlockState[] states = new BlockState[Math.max(0, limit - bottom)];
        for (int index = 0; index < states.length; index++) {
            states[index] = blockAt(columns, column, surface, x, bottom + index, z);
        }
        return new NoiseColumn(bottom, states);
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> lines, RandomState randomState,
                                   BlockPos pos) {
        TerrainColumns sampler = columns;
        lines.add("dndclasses terrain seed=" + sampler.seed());
        TerrainColumns.Column column = sampler.sample(pos.getX(), pos.getZ());
        lines.add("dndclasses terrain biome=" + column.biome() + " ground=" + column.ground());
    }
}
