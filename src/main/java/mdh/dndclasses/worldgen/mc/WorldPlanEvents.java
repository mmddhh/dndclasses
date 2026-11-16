package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.Dndclasses;
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
    public static void onServerStopping(ServerStoppingEvent event) {
        WorldPlanCache.clear();
    }
}
