package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.WeaponProperty;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses the main-hand item-use animation when the right click throws the offhand weapon. */
@Mixin(ItemInHandRenderer.class)
public abstract class OffhandThrowItemUsedMixin {

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void dndclasses$skipMainHandUseAnimation(InteractionHand hand, CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof DndWeapon weapon
                && weapon.spec().isThrown() && weapon.spec().has(WeaponProperty.LIGHT)) {
            ci.cancel();
        }
    }
}
