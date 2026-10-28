package mdh.dndclasses.data;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.events.LevelUp;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Offers the subclass picker when a character reaches its subclass level (and on login). */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SubclassEvents {

    private SubclassEvents() {
    }

    @SubscribeEvent
    public static void onLevelUp(LevelUp event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            SubclassSelection.sendPrompt(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SubclassSelection.sendPrompt(player);
        }
    }
}
