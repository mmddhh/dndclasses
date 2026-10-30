package mdh.dndclasses.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
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

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void dndclasses$blockUnchargedAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft self = (Minecraft) (Object) this;
        if (self.player == null || self.hitResult == null) {
            return;
        }
        float charge = self.player.getAttackStrengthScale(0.5F);
        HitResult.Type type = self.hitResult.getType();
        LOGGER.info("DND startAttack: hit={} charge={}", type, charge);
        boolean gated = self.hitResult instanceof EntityHitResult || type == HitResult.Type.MISS;
        if (gated && charge < 0.999F) {
            LOGGER.info("DND startAttack blocked: hit={} charge={}", type, charge);
            cir.setReturnValue(false);
        }
    }
}
