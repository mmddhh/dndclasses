package mdh.dndclasses.mixin;

import mdh.dndclasses.gear.DndDurability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * D&amp;D gear never disappears: when durability would reach zero the item becomes broken
 * (damage = max - 1 + DndBroken tag) instead of being destroyed.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true)
    private void dndclasses$preventBreaking(int amount, LivingEntity entity, Consumer<LivingEntity> onBroken, CallbackInfo callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (!DndDurability.isDndGear(self)) {
            return;
        }
        if (DndDurability.isBroken(self)) {
            callback.cancel();
            return;
        }
        int max = self.getMaxDamage();
        if (max > 0 && self.getDamageValue() + amount >= max) {
            self.setDamageValue(Math.max(0, max - 1));
            DndDurability.setBroken(self, true);
            callback.cancel();
        }
    }
}
