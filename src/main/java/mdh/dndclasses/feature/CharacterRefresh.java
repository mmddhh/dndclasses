package mdh.dndclasses.feature;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.spellcasting.SpellcastingManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Single entry point that recomputes every derived character effect (features + spellcasting). */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CharacterRefresh {

    private CharacterRefresh() {
    }

    public static void refresh(ServerPlayer player) {
        FeatureManager.refreshAll(player);
        SpellcastingManager.refresh(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FeatureManager.clearApplied(player.getUUID());
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FeatureManager.clearApplied(event.getEntity().getUUID());
    }
}
