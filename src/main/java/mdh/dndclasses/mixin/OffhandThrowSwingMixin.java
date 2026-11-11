package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.WeaponProperty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Final guard for the main-hand animation: while a right click is being handled as an offhand
 * throw, any main-hand swing is dropped on both sides. Vanilla (and other mods) can start a
 * main-hand swing through paths that are not the use result, so suppressing the swing itself is
 * the only reliable way to keep the main hand still.
 */
@Mixin(LivingEntity.class)
public abstract class OffhandThrowSwingMixin {

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void dndclasses$cancelMainHandSwing(InteractionHand hand, boolean sendToSelf, CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof DndWeapon weapon
                && weapon.spec().isThrown() && weapon.spec().has(WeaponProperty.LIGHT)
                && DndCooldowns.isThrowWindow(player, 2)) {
            ci.cancel();
        }
    }
}
