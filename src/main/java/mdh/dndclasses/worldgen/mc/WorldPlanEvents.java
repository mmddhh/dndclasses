package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.Dndclasses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Publishes the persisted plan to the chunk-thread cache before chunks are generated. */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldPlanEvents {

    private WorldPlanEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        WorldPlanCache.set(WorldPlanSavedData.get(event.getServer().overworld()).plan());
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        mdh.dndclasses.worldgen.terrain.TerrainBiomeSource.install(
                event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        mdh.dndclasses.worldgen.terrain.HydrologyService.clear();
        WorldPlanCache.clear();
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        mdh.dndclasses.worldgen.terrain.TerrainBiomeSource.install(
                event.getLevel().registryAccess());
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean snapshot = level.getChunkSource().getGenerator() instanceof TerrainChunkGenerator terrain
                && terrain.usesSnapshot();
        if (!snapshot && !level.dimension().location().equals(
                new ResourceLocation(Dndclasses.MODID, "dnd_overworld"))) {
            return;
        }
        if (level.getChunkSource().getGenerator() instanceof TerrainChunkGenerator generator) {
            generator.setWorldSeed(level.getSeed());
        }
        WorldBorder border = level.getWorldBorder();
        border.setCenter(0.0D, 0.0D);
        border.setSize(62_720.0D);
    }
}
