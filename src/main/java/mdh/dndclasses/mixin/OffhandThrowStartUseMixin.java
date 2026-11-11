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

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void dndclasses$cancelUnreadyThrow(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        Player player = self.player;
        if (player == null) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof DndWeapon weapon)
                || !weapon.spec().isThrown() || !weapon.spec().has(WeaponProperty.LIGHT)) {
            return;
        }
        DndCooldowns.markThrowWindow(player);
        if (player.getAttackStrengthScale(0.5F) < 0.999F
                || DndCooldowns.isRecentAttack(player, 5)) {
            ci.cancel();
        }
    }
}
