package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.compat.BetterCombatCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void dndclasses$cancelCoolingSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer player)) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (BetterCombatCompat.isActive()) {
            // Better Combat drives the swing animation and attack timing itself.
            return;
        }
        if (stack.getItem() instanceof DndWeapon) {
            // Swing only when the attack is actually allowed: uncharged swings are dropped so the
            // rejected attack has no hand animation.
            if (player.getAttackStrengthScale(0.5F) < 0.999F
                    || !DndCooldowns.tryUse(player, hand)) {
                ci.cancel();
            }
        }
    }
}
