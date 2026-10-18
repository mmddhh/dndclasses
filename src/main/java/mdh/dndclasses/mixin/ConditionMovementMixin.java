package mdh.dndclasses.mixin;

import mdh.dndclasses.client.ClientConditionState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-side movement lock for D&amp;D conditions (immobile / no jump). */
@Mixin(LivingEntity.class)
public abstract class ConditionMovementMixin {

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true, index = 1)
    private Vec3 dndclasses$lockMovement(Vec3 vector) {
        if (((Object) this) instanceof LocalPlayer && ClientConditionState.isImmobile()) {
            return Vec3.ZERO;
        }
        return vector;
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void dndclasses$lockJump(CallbackInfo ci) {
        if (((Object) this) instanceof LocalPlayer && ClientConditionState.isImmobile()) {
            ci.cancel();
        }
    }
}
