package mdh.dndclasses.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels an uncharged attack before Minecraft swings the arm.
 * <p>
 * Entity attacks and air swings are both gated: with the indicator not full nothing happens at all
 * (no damage, no arm swing). Block breaking is deliberately left alone so mining keeps working.
 * <p>
 * {@code Minecraft#startAttack} returns {@code boolean}, hence the returnable callback.
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
}
