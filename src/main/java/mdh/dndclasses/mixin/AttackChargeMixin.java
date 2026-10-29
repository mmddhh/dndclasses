package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DualWieldRules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla lets the player attack at any charge and only scales the damage down. Instead, require a
 * fully charged attack: the swing (and the attack packet) is dropped until the indicator is full.
 * <p>
 * The attack speed itself is 10 ticks for every D&amp;D weapon and 5 ticks while dual wielding
 * (see {@link DualWieldRules#updateAttackSpeed}), so this is a 0.5 s / 0.25 s cadence.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class AttackChargeMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void dndclasses$requireFullCharge(Player player, Entity target, CallbackInfo ci) {
        if (player.getAttackStrengthScale(0.5F) < 0.999F) {
            ci.cancel();
        }
    }
}
