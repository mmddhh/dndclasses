package mdh.dndclasses.mixin;

import com.mojang.logging.LogUtils;
import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.WeaponProperty;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
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

    private static final Logger LOGGER = LogUtils.getLogger();

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
        LOGGER.info("DND useItem HEAD offhandThrow=true recent={}", recent);
        if (recent) {
            // CONSUME stops Minecraft.startUseItem from falling through to the offhand and from
            // swinging the main hand; no use packet is sent, so the throw is fully blocked.
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
        if (offhand.getItem() instanceof DndWeapon weapon
                && weapon.spec().isThrown() && weapon.spec().has(WeaponProperty.LIGHT)) {
            // The use packet has already been sent; CONSUME keeps startUseItem from iterating the
            // offhand (second use packet) and from playing the main-hand swing animation.
            LOGGER.info("DND useItem RETURN suppress main swing");
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
