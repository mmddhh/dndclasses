package mdh.dndclasses.mixin;

import mdh.dndclasses.compat.BetterCombatCompat;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces the generic {@code playerAttack} damage source with the weapon's D&amp;D damage type.
 * <p>
 * This is needed for attacks that go through vanilla {@code Player.attack} (Better Combat mode),
 * because {@code LivingHurtEvent} cannot change the source. Our own attack path already passes the
 * correct source, so re-replacing it with the same type is harmless.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtTypeMixin {

    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private DamageSource dndclasses$useDndDamageType(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return source;
        }
        if (source.getDirectEntity() != player) {
            return source;
        }
        ItemStack stack = BetterCombatCompat.isOffHandAttack(player)
                ? player.getOffhandItem()
                : player.getMainHandItem();
        if (!(stack.getItem() instanceof DndWeapon weapon)) {
            return source;
        }
        return ModDamageTypes.source(player.level(), weapon.spec().damageType(), player, player);
    }
}
