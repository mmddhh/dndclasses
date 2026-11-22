package mdh.dndclasses.worldgen.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/** Biome source backed by the same TerrainDensity surface used for blocks. */
public final class TerrainBiomeSource extends BiomeSource {

    public static final Codec<TerrainBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.optionalFieldOf("seed", 12_345L).forGetter(source -> source.seed)
            ).apply(instance, TerrainBiomeSource::new));

    private static volatile Map<TerrainModel.Biome, Holder<Biome>> holders = Map.of();
    private static volatile Holder<Biome> fallback;
    private static volatile Holder<Biome> river;

    private long seed;
    private TerrainModel model;
    private TerrainDensity density;

    public TerrainBiomeSource(long seed) {
        this.seed = seed;
        rebuild();
    }

    public void setSeed(long seed) {
        if (seed != this.seed) {
            this.seed = seed;
            rebuild();
        }
    }

    private void rebuild() {
        this.model = new TerrainModel(WorldTopology.standard(), seed);
        this.density = new TerrainDensity(WorldTopology.standard(), seed);
    }

    public static void install(RegistryAccess access) {
        Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);
        EnumMap<TerrainModel.Biome, Holder<Biome>> map = new EnumMap<>(TerrainModel.Biome.class);
        map.put(TerrainModel.Biome.OCEAN, holder(registry, "ocean"));
        map.put(TerrainModel.Biome.BEACH, holder(registry, "beach"));
        map.put(TerrainModel.Biome.TUNDRA, holder(registry, "snowy_plains"));
        map.put(TerrainModel.Biome.TAIGA, holder(registry, "taiga"));
        map.put(TerrainModel.Biome.FOREST, holder(registry, "forest"));
        map.put(TerrainModel.Biome.PLAINS, holder(registry, "plains"));
        map.put(TerrainModel.Biome.SAVANNA, holder(registry, "savanna"));
        map.put(TerrainModel.Biome.JUNGLE, holder(registry, "jungle"));
        map.put(TerrainModel.Biome.SWAMP, holder(registry, "swamp"));
        map.put(TerrainModel.Biome.DESERT, holder(registry, "desert"));
        map.put(TerrainModel.Biome.HILLS, holder(registry, "windswept_hills"));
        map.put(TerrainModel.Biome.MOUNTAINS, holder(registry, "jagged_peaks"));
        holders = Map.copyOf(map);
        fallback = holder(registry, "plains");
        river = holder(registry, "river");
    }

    private static Holder<Biome> holder(Registry<Biome> registry, String path) {
        return registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,
                new ResourceLocation(path)));
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return holders.values().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ,
                                       Climate.Sampler sampler) {
        int x = quartX << 2;
        int z = quartZ << 2;
        if (RegionalHydrology.hit(x, z, seed) != null) {
            return river;
        }
        int surface = density.surfaceHeight(x, z);
        TerrainModel.Biome kind = model.biome(x, z, surface);
        Holder<Biome> holder = holders.get(kind);
        return holder != null ? holder : fallback;
    }
}
