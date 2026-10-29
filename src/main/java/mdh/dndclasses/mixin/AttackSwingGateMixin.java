package mdh.dndclasses.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels an uncharged attack against an entity before Minecraft swings the arm.
 * <p>
 * Only entity attacks are blocked: block breaking and air swings keep working, so mining is not
 * affected. Combined with the {@code MultiPlayerGameMode.attack} guard this means an uncharged
 * click produces neither damage nor the attack animation.
 * <p>
 * {@code Minecraft#startAttack} returns {@code boolean}, hence the returnable callback.
 */
@Mixin(Minecraft.class)
public abstract class AttackSwingGateMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void dndclasses$blockUnchargedAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft self = (Minecraft) (Object) this;
        if (self.player == null) {
            return;
        }
        float charge = self.player.getAttackStrengthScale(0.5F);
        LOGGER.info("DND startAttack: hit={} charge={}",
                self.hitResult == null ? "null" : self.hitResult.getType(), charge);
        if (self.hitResult instanceof EntityHitResult && charge < 0.999F) {
            LOGGER.info("DND startAttack blocked: charge={}", charge);
            cir.setReturnValue(false);
        }
    }
}
