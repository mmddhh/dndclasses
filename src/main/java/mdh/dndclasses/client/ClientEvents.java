package mdh.dndclasses.client;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.weapon.DndCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
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
        RollAnimationState.tick(minecraft);
        ClientReloadState.tick();
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
                // Mirror the server charge locally so the crossbow model shows the loading frames.
                InteractionHand hand = DndCombat.findCrossbowHand(minecraft.player);
                if (hand != null && !ClientReloadState.isCharging()
                        && DndCombat.canReloadCrossbow(minecraft.player, hand)) {
                    ClientReloadState.start(CrossbowItem.getChargeDuration(
                            minecraft.player.getItemInHand(hand)));
                }
                mdh.dndclasses.network.DndNetwork.CHANNEL.sendToServer(
                        new mdh.dndclasses.network.ReloadWeaponPacket());
            }
        }

        while (ClientSetup.ROLL.consumeClick()) {
            if (minecraft.getConnection() != null) {
                float forward = minecraft.player.input.forwardImpulse;
                float strafe = minecraft.player.input.leftImpulse;
                if (forward == 0.0F && strafe == 0.0F) {
                    forward = 1.0F;
                }
                float yaw = (float) Math.toRadians(minecraft.player.getYRot());
                float dirX = (float) (strafe * Math.cos(yaw) - forward * Math.sin(yaw));
                float dirZ = (float) (strafe * Math.sin(yaw) + forward * Math.cos(yaw));
                float length = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (length > 1.0E-4F) {
                    dirX /= length;
                    dirZ /= length;
                }
                mdh.dndclasses.network.DndNetwork.CHANNEL.sendToServer(
                        new mdh.dndclasses.network.RollPacket(dirX, dirZ));
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
