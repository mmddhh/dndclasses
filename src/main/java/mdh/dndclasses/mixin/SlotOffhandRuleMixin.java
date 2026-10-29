package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.DualWieldRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rejects an illegal weapon at the moment it is put into the offhand slot (click, shift-click,
 * drag). The item simply stays where it was: nothing is moved or copied, so this cannot duplicate
 * stacks and Better Combat never sees an illegal dual-wield pair.
 */
@Mixin(Slot.class)
public abstract class SlotOffhandRuleMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void dndclasses$offhandRule(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!self.getClass().getName().contains("OffhandSlot")) {
            return;
        }
        if (!(self.container instanceof Inventory inventory)) {
            return;
        }
        Player player = inventory.player;
        if (stack.getItem() instanceof DndWeapon && !DualWieldRules.canHoldInOffHand(player, stack)) {
            cir.setReturnValue(false);
        }
    }
}
