package mdh.dndclasses.worldgen.mc;

import com.mojang.serialization.Codec;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.worldgen.terrain.TerrainBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers the finite terrain generator used by the test dimension. */
public final class TerrainWorldgen {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Dndclasses.MODID);
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Dndclasses.MODID);

    public static final RegistryObject<Codec<TerrainChunkGenerator>> TERRAIN =
            GENERATORS.register("terrain", () -> TerrainChunkGenerator.CODEC);
    public static final RegistryObject<Codec<TerrainBiomeSource>> TERRAIN_BIOMES =
            BIOME_SOURCES.register("terrain_biomes", () -> TerrainBiomeSource.CODEC);

    private TerrainWorldgen() {
    }

    public static void register(IEventBus bus) {
        GENERATORS.register(bus);
        BIOME_SOURCES.register(bus);
    }
}
