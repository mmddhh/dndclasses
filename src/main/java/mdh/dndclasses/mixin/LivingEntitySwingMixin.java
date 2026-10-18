package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeaponItem;
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
        if (stack.getItem() instanceof DndWeaponItem && !DndCooldowns.tryUse(player, hand)) {
            ci.cancel();
        }
    }
}
