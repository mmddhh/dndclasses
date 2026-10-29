package mdh.dndclasses.compat;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.feature.FeatureEffects;
import mdh.dndclasses.gear.DndDurability;
import mdh.dndclasses.weapon.DndAttributes;
import mdh.dndclasses.weapon.DndCombat;
import mdh.dndclasses.weapon.DndDamageType;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.DndWeaponMaterial;
import mdh.dndclasses.weapon.DndWeaponSpec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Damage layer for Better Combat mode.
 * <p>
 * Better Combat resolves one vanilla {@code Player.attack} per target, which fires
 * {@link LivingHurtEvent}. The damage amount is fully replaced with the D&amp;D result so that
 * Better Combat's global multipliers (combo, dual wielding, reworked sweeping) never leak into
 * the game's numbers. The damage source itself is replaced separately by
 * {@code LivingEntityHurtTypeMixin}.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BetterCombatDamageHandler {

    private BetterCombatDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!BetterCombatCompat.isActive()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSource().getDirectEntity() != player) {
            return;
        }
        LivingEntity target = event.getEntity();

        boolean offHand = BetterCombatCompat.isOffHandAttack(player);
        ItemStack stack = offHand ? player.getOffhandItem() : player.getMainHandItem();
        if (!(stack.getItem() instanceof DndWeapon weapon)) {
            return;
        }
        DndWeaponSpec spec = weapon.spec();

        DndDamageType damageType = spec.damageType();
        boolean crit = false;
        int damage;
        if (DndDurability.isBroken(stack)) {
            damage = DndCombat.minimumDamage(1 + DndAttributes.strengthModifier(player));
            damageType = DndDamageType.BLUDGEONING;
        } else if (spec.isRanged()) {
            damage = DndCombat.minimumDamage(2 + DndAttributes.strengthModifier(player));
        } else {
            boolean offhandEmpty = player.getOffhandItem().isEmpty();
            int base = spec.damage(offhandEmpty);
            int modifier = DndAttributes.modifier(player, spec.abilityMode());
            if (offHand) {
                modifier = Math.min(modifier, 0);
            }
            crit = player.getRandom().nextFloat() < 0.05F
                    || (spec.material() == DndWeaponMaterial.ORICHALCUM && DndCombat.isConstruct(target));
            damage = DndCombat.minimumDamage(crit ? base * 2 + modifier : base + modifier);
        }

        int bonus = (int) Math.round(FeatureEffects.attackBonus(player));
        if (bonus != 0) {
            damage = DndCombat.minimumDamage(damage + bonus);
        }
        int hits = offHand ? 1 : 1 + FeatureEffects.extraAttacks(player);
        if (hits > 1) {
            damage = DndCombat.minimumDamage(damage * hits);
        }

        event.setAmount(damage);

        if (crit && target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }
}
