package mdh.dndclasses.worldgen.mc;

import com.mojang.serialization.Codec;
import mdh.dndclasses.Dndclasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers the finite terrain generator used by the test dimension. */
public final class TerrainWorldgen {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Dndclasses.MODID);

    public static final RegistryObject<Codec<TerrainChunkGenerator>> TERRAIN =
            GENERATORS.register("terrain", () -> TerrainChunkGenerator.CODEC);

    private TerrainWorldgen() {
    }

    public static void register(IEventBus bus) {
        GENERATORS.register(bus);
    }
}
