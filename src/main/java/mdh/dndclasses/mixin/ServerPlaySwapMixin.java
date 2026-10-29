package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.DualWieldRules;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the vanilla "swap item with offhand" action (F key) when the swap would put an illegal
 * weapon in the offhand. Cancelling before the server performs the swap avoids the partial-swap
 * duplication that a per-slot rejection would cause.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlaySwapMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void dndclasses$rejectIllegalOffhandSwap(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return;
        }
        ItemStack toOffHand = player.getMainHandItem();
        if (!(toOffHand.getItem() instanceof DndWeapon)) {
            return;
        }
        ItemStack futureMain = player.getOffhandItem();
        boolean mainAllows = !(futureMain.getItem() instanceof DndWeapon)
                || DualWieldRules.isLightWeapon(futureMain);
        if (!DualWieldRules.isLightWeapon(toOffHand) || !mainAllows) {
            ci.cancel();
            player.displayClientMessage(
                    Component.translatable("message.dndclasses.offhand_light_only"), true);
        }
    }
}
