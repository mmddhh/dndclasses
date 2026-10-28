package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndWeapon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla's first-person hand selection only recognizes {@code Items.BOW}/{@code Items.CROSSBOW}
 * by exact item id. Treat our D&amp;D bows/crossbows as bow-like so only the used hand is rendered
 * with the vanilla pose.
 */
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Redirect(
            method = "selectionUsingItemWhileHoldingBowLike",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0)
    )
    private static boolean dndclasses$isBowLike(ItemStack stack, Item item) {
        return stack.is(item)
                || (item == Items.BOW && stack.getItem() instanceof DndWeapon weapon && weapon.spec().isBow());
    }

    @Redirect(
            method = "selectionUsingItemWhileHoldingBowLike",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 1)
    )
    private static boolean dndclasses$isCrossbowLike(ItemStack stack, Item item) {
        return stack.is(item)
                || (item == Items.CROSSBOW && stack.getItem() instanceof DndWeapon weapon && weapon.spec().isCrossbow());
    }
}
