package mdh.dndclasses.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla armor points are disabled for players: AC comes from the armors/armor module. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityArmorMixin {

    @Inject(method = "getArmorValue", at = @At("HEAD"), cancellable = true)
    private void dndclasses$disableVanillaArmor(CallbackInfoReturnable<Integer> callback) {
        if ((Object) this instanceof Player) {
            callback.setReturnValue(0);
        }
    }
}
