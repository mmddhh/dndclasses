package mdh.dndclasses.mixin;

import mdh.dndclasses.client.ClientDualWield;
import mdh.dndclasses.weapon.DualWieldRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels an uncharged attack before Minecraft swings the arm, and alternates the visible hand on
 * air swings while dual wielding.
 * <p>
 * Entity attacks and air swings are both gated: with the indicator not full nothing happens at all
 * (no damage, no arm swing). Block breaking is deliberately left alone so mining keeps working.
 * <p>
 * Air swings never reach the server ({@link net.minecraftforge.event.entity.player.AttackEntityEvent}
 * only fires for entities), so the hand shown there is decided locally from
 * {@link ClientDualWield}; entity attacks stay server-authoritative via {@code SwingHandPacket}.
 * <p>
 * {@code Minecraft#startAttack} returns {@code boolean}, hence the returnable callbacks.
 */
@Mixin(Minecraft.class)
public abstract class AttackSwingGateMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void dndclasses$blockUnchargedAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft self = (Minecraft) (Object) this;
        if (self.player == null || self.hitResult == null) {
            return;
        }
        HitResult.Type type = self.hitResult.getType();
        boolean gated = self.hitResult instanceof EntityHitResult || type == HitResult.Type.MISS;
        if (gated && self.player.getAttackStrengthScale(0.5F) < 0.999F) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Runs after vanilla has swung the main hand and reset the attack charge for a MISS. Replaces
     * that swing with the offhand when it is the offhand's turn.
     */
    @Inject(method = "startAttack", at = @At("TAIL"))
    private void dndclasses$offHandAirSwing(CallbackInfoReturnable<Boolean> cir) {
        Minecraft self = (Minecraft) (Object) this;
        LocalPlayer player = self.player;
        if (player == null || self.hitResult == null) {
            return;
        }
        if (self.hitResult.getType() != HitResult.Type.MISS) {
            return;
        }
        if (!DualWieldRules.canDualWield(player)) {
            return;
        }
        if (ClientDualWield.nextHand(player) == InteractionHand.OFF_HAND) {
            // Clear the swing that vanilla just started so the offhand replay is not ignored.
            player.swinging = false;
            player.swing(InteractionHand.OFF_HAND, false);
        }
        ClientDualWield.flip(player);
    }
}
