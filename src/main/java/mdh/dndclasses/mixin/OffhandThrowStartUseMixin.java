package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.WeaponProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the whole right-click use before vanilla sends the use packet or plays the main-hand
 * "item used" animation, while the offhand throw is not ready (attack indicator not full, or a
 * melee attack just happened). Otherwise vanilla keeps re-running the use path while the button is
 * held: the main hand animates forever and the throw is refused on the server.
 */
@Mixin(Minecraft.class)
public abstract class OffhandThrowStartUseMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void dndclasses$resetThrowOnRelease(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        if (self.options != null && !self.options.keyUse.isDown()) {
            mdh.dndclasses.client.OffhandThrowState.reset();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void dndclasses$cancelUnreadyThrow(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        Player player = self.player;
        if (player == null) {
            return;
        }
        if (mdh.dndclasses.client.OffhandThrowState.isCoolingDown(player.level().getGameTime())) {
            // A throw is still cooling down: cancel the use path completely, even when both hands
            // are empty, so no predicted hand swing is played.
            ci.cancel();
            return;
        }
        // One press = one throw. While the key stays held after a throw, skip the entire use path
        // so the main hand is not thrown/animated when the offhand has become empty. The flag is
        // cleared in tick() once the key is actually released.
        if (mdh.dndclasses.client.OffhandThrowState.isConsumed() && self.options.keyUse.isDown()) {
            ci.cancel();
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        boolean offhandThrow = offhand.getItem() instanceof DndWeapon offHandWeapon
                && offHandWeapon.spec().isThrown() && offHandWeapon.spec().has(WeaponProperty.LIGHT);
        ItemStack main = player.getMainHandItem();
        boolean mainThrow = main.getItem() instanceof DndWeapon mainWeapon && mainWeapon.spec().isThrown();
        if (!offhandThrow && !mainThrow) {
            return;
        }
        // Cooldown window: do nothing at all (no packet, no main-hand swing animation).
        if (player.getAttackStrengthScale(0.5F) < 0.999F
                || DndCooldowns.isRecentAttack(player, 5)) {
            ci.cancel();
            return;
        }
        // Consume the charge locally too, even when the empty main hand means the use packet is
        // sent for the offhand: otherwise the client would keep the indicator full and let the
        // next right click run the use path (offhand swing) although the server is on cooldown.
        player.resetAttackStrengthTicker();
        // One press = one throw; tick() clears this once the use key is released.
        mdh.dndclasses.client.OffhandThrowState.markConsumed();
        mdh.dndclasses.client.OffhandThrowState.markThrown(player.level().getGameTime(),
                mdh.dndclasses.weapon.DualWieldRules.canDualWield(player) ? 10 : 20);
    }
}
