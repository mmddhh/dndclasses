package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.WeaponProperty;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-click quality of life for the offhand throw:
 * <ul>
 *   <li>a use right after a melee attack input is dropped entirely (no use packet), so a fast
 *       left-then-right click cannot throw during the attack window;</li>
 *   <li>when the offhand holds a light thrown D&amp;D weapon, the predicted main-hand swing is
 *       suppressed after the use packet is sent (the server swings the offhand instead).</li>
 * </ul>
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class OffhandThrowUseMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void dndclasses$blockThrowRightAfterAttack(Player player, InteractionHand hand,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        boolean offhandThrow = offhand.getItem() instanceof DndWeapon weapon
                && weapon.spec().isThrown() && weapon.spec().has(WeaponProperty.LIGHT);
        if (!offhandThrow) {
            return;
        }
        boolean recent = DndCooldowns.isRecentAttack(player, 5);
        boolean charged = player.getAttackStrengthScale(0.5F) >= 0.999F;
        if (recent || !charged) {
            // CONSUME stops Minecraft.startUseItem from falling through to the offhand and from
            // swinging the main hand; no use packet is sent, so the throw is fully blocked.
            // The attack-strength check ties throwing to the attack indicator: after a melee
            // attack the throw stays locked until the indicator is full again.
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "useItem", at = @At("RETURN"), cancellable = true)
    private void dndclasses$suppressMainHandSwing(Player player, InteractionHand hand,
                                                 CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        ItemStack main = player.getMainHandItem();
        boolean offhandThrow = offhand.getItem() instanceof DndWeapon offWeapon
                && offWeapon.spec().isThrown() && offWeapon.spec().has(WeaponProperty.LIGHT);
        boolean mainThrow = main.getItem() instanceof DndWeapon mainWeapon && mainWeapon.spec().isThrown();
        if (!offhandThrow && !mainThrow) {
            return;
        }
        if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
            localPlayer.resetAttackStrengthTicker();
            mdh.dndclasses.client.OffhandThrowState.markConsumed();
        }
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
