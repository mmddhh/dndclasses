package mdh.dndclasses.client;

import mdh.dndclasses.Dndclasses;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {

    private static boolean pendingOpen = false;
    private static int pendingTicks = 0;

    private ClientEvents() {
    }

    public static void requestOpen() {
        pendingOpen = true;
        pendingTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (pendingOpen) {
            if (minecraft.screen == null) {
                pendingTicks++;
                if (pendingTicks > 40) {
                    pendingOpen = false;
                    minecraft.setScreen(new ClassCreationScreen());
                    return;
                }
            } else {
                pendingTicks = 0;
            }
        }

        while (ClientSetup.OPEN_CLASS_SCREEN.consumeClick()) {
            if (!ClientCharacterState.created) {
                minecraft.setScreen(new ClassCreationScreen());
            } else {
                minecraft.setScreen(new CharacterSheetScreen());
            }
        }

        while (ClientSetup.OPEN_FEATURE_WHEEL.consumeClick()) {
            if (ClientCharacterState.created) {
                FeatureWheelOverlay.toggle();
            } else {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.dndclasses.feature.need_class"), true);
            }
        }

        while (ClientSetup.RELOAD_WEAPON.consumeClick()) {
            if (minecraft.getConnection() != null) {
                mdh.dndclasses.network.DndNetwork.CHANNEL.sendToServer(
                        new mdh.dndclasses.network.ReloadWeaponPacket());
            }
        }
    }

    /** Hide the vanilla chat panel while a dndclasses overlay (wheel / save QTE) is open. */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (FeatureWheelOverlay.isOpen() || SaveQteOverlay.isOpen()) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
                event.setCanceled(true);
            }
        }
    }

    /** AUI overlays draw on top of vanilla Screens, so close ours whenever a Screen opens. */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() == null) {
            return;
        }
        if (FeatureWheelOverlay.isOpen()) {
            FeatureWheelOverlay.close(false);
        }
        if (SaveQteOverlay.isOpen()) {
            SaveQteOverlay.close();
        }
    }
}
