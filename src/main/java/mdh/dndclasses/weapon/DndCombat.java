package mdh.dndclasses.weapon;

import mdh.dndclasses.feature.FeatureEffects;
import mdh.dndclasses.gear.DndDurability;
import mdh.dndclasses.weapon.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;

import java.util.Optional;

public final class DndCombat {

    private static final String TAG_RELOAD_END = "DndReloadEnd";
    private static final String TAG_RELOAD_HAND = "DndReloadHand";

    private DndCombat() {
    }

    public static boolean attack(Player player, Entity target, InteractionHand hand, boolean rangedAsMelee) {
        if (player.level().isClientSide || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (!DndCooldowns.tryUse(player, hand)) {
            return true;
        }

        ItemStack stack = player.getItemInHand(hand);
        DndDamageType damageType = DndDamageType.BLUDGEONING;
        boolean crit = false;
        boolean brokenWeapon = DndDurability.isBroken(stack);
        int damage;
        if (stack.getItem() instanceof DndWeapon weaponItem) {
            DndWeaponSpec spec = weaponItem.spec();
            if (!brokenWeapon && !canUseWeapon(player, hand, spec, true)) {
                return true;
            }
            if (brokenWeapon) {
                damage = minimumDamage(1 + DndAttributes.strengthModifier(player));
            } else if (rangedAsMelee && spec.isRanged()) {
                damage = minimumDamage(2 + DndAttributes.strengthModifier(player));
            } else {
                boolean offhandEmpty = player.getOffhandItem().isEmpty();
                int base = spec.damage(offhandEmpty);
                int modifier = DndAttributes.modifier(player, spec.abilityMode());
                if (hand == InteractionHand.OFF_HAND) {
                    // D&D two-weapon fighting: the offhand attack does not add a positive ability
                    // modifier (a negative one still applies).
                    modifier = Math.min(modifier, 0);
                }
                crit = player.getRandom().nextFloat() < 0.05F
                        || (spec.material() == DndWeaponMaterial.ORICHALCUM && isConstruct(livingTarget));
                damage = minimumDamage(crit ? base * 2 + modifier : base + modifier);
                damageType = spec.damageType();
            }
            if (!brokenWeapon) {
                stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
            }
        } else if (VanillaEquipmentRules.isAllowedTool(stack.getItem())) {
            damage = minimumDamage(2 + DndAttributes.strengthModifier(player));
            damageType = VanillaEquipmentRules.toolDamageType(stack.getItem());
            stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
        } else if (stack.isEmpty()) {
            damage = minimumDamage(1 + DndAttributes.strengthModifier(player));
        } else {
            if (VanillaEquipmentRules.isForbidden(stack.getItem())) {
                player.displayClientMessage(Component.translatable("message.dndclasses.disabled_item"), true);
                return true;
            }
            damage = minimumDamage(1 + DndAttributes.strengthModifier(player));
        }

        if (player instanceof ServerPlayer serverPlayer) {
            int bonus = (int) Math.round(FeatureEffects.attackBonus(serverPlayer));
            if (bonus != 0) {
                damage = minimumDamage(damage + bonus);
            }
            // Extra Attack only ever applies to the main-hand attack; the offhand swing is the
            // bonus-action attack and stays a single hit.
            int hits = hand == InteractionHand.MAIN_HAND
                    ? 1 + FeatureEffects.extraAttacks(serverPlayer)
                    : 1;
            if (hits > 1) {
                // One damage roll per attack: extra attacks add their own damage to this hit,
                // which also repeats the ability modifier just like separate attacks would.
                damage = minimumDamage(damage * hits);
            }
        }

        livingTarget.hurt(ModDamageTypes.source(player.level(), damageType, player, player), damage);
        if (crit && livingTarget.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5D, livingTarget.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.1D);
        }
        player.swing(hand, true);
        if (hand == InteractionHand.OFF_HAND) {
            // Vanilla's first-person renderer does animate the offhand (renderHandsWithItems passes
            // the swing progress for the swinging arm), so no extra main-hand swing is needed; just
            // add a distinct whoosh + sweep particle so the bonus attack reads clearly.
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.4F);
            if (livingTarget.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5D,
                        livingTarget.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        player.resetAttackStrengthTicker();
        return true;
    }

    public static boolean throwWeapon(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeapon weaponItem) || !weaponItem.spec().isThrown()) {
            return false;
        }
        if (DndDurability.isBroken(stack)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.broken"), true);
            return true;
        }
        if (!DndCooldowns.tryUse(player, hand) || !canUseWeapon(player, hand, weaponItem.spec(), false)) {
            return true;
        }

        ItemStack thrown = stack.copy();
        thrown.setCount(1);
        stack.shrink(1);
        player.setItemInHand(hand, stack);

        ThrownWeaponEntity entity = new ThrownWeaponEntity(player.level(), player, thrown);
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.7F, 1.0F);
        player.level().addFreshEntity(entity);
        player.swing(hand, true);
        return true;
    }

    public static boolean shootRanged(Player player, InteractionHand hand) {
        return shootRanged(player, hand, 2.6F);
    }

    public static boolean shootRanged(Player player, InteractionHand hand, float speed) {
        if (player.level().isClientSide) {
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeapon weaponItem) || !weaponItem.spec().isRanged()) {
            return false;
        }
        DndWeaponSpec spec = weaponItem.spec();
        if (DndDurability.isBroken(stack)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.broken"), true);
            return true;
        }
        if (!DndCooldowns.tryUse(player, hand) || !canUseWeapon(player, hand, spec, false)) {
            return true;
        }
        if (spec.isLoading() && !stack.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.not_loaded"), true);
            return true;
        }

        Optional<ItemStack> ammo = takeAmmoForShot(player, stack, spec);
        if (ammo.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dndclasses.no_ammo"), true);
            return true;
        }

        if (spec.isBow() || spec.isCrossbow()) {
            DndArrowEntity entity = new DndArrowEntity(player.level(), player, ammo.get(), spec);
            float launchSpeed = spec.isCrossbow() ? 3.0F : speed;
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, launchSpeed, 0.5F);
            player.level().addFreshEntity(entity);
        } else {
            ThrownWeaponEntity entity = new ThrownWeaponEntity(player.level(), player, ammo.get(), spec);
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.6F, 0.5F);
            player.level().addFreshEntity(entity);
        }
        if (spec.isLoading()) {
            stack.getOrCreateTag().putBoolean(DndWeaponItem.TAG_LOADED, false);
            stack.getOrCreateTag().remove(DndWeaponItem.TAG_LOADED_AMMO);
            if (spec.isCrossbow()) {
                CrossbowItem.setCharged(stack, false);
            }
        }
        if (!spec.isBow()) {
            player.swing(hand, true);
        }
        return true;
    }

    public static boolean reload(Player player) {
        if (!(player.level() instanceof ServerLevel)) {
            return false;
        }
        if (reloadHand(player, InteractionHand.MAIN_HAND)) {
            return true;
        }
        return reloadHand(player, InteractionHand.OFF_HAND);
    }

    private static boolean reloadHand(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeapon weaponItem) || !weaponItem.spec().isLoading()) {
            return false;
        }
        return completeLoading(player, hand);
    }

    public static InteractionHand findCrossbowHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof DndWeapon weaponItem && weaponItem.spec().isCrossbow()) {
                return hand;
            }
        }
        return null;
    }

    public static boolean canReloadCrossbow(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeapon weaponItem) || !weaponItem.spec().isCrossbow()) {
            return false;
        }
        return !stack.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED)
                && hasAmmo(player, ammoSuffixFor(weaponItem.spec()));
    }

    /**
     * Starts a vanilla crossbow charge: the same {@code UseAnim.CROSSBOW} animation, the same
     * charge duration as vanilla ({@link CrossbowItem#getChargeDuration}) and the same rule that a
     * crossbow cannot fire until the charge finishes, which is what stops rapid fire.
     * <p>
     * The completion is timed by the server clock in {@link #tickReload} rather than by the use
     * state alone: cancelling the animation (right click, item swap, ...) no longer wastes the
     * charge, and a single key press is always enough.
     */
    public static boolean startReload(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        InteractionHand hand = findCrossbowHand(player);
        if (hand == null) {
            return reload(player);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeapon weaponItem) || !weaponItem.spec().isCrossbow()) {
            return false;
        }
        if (stack.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.already_loaded"), true);
            return true;
        }
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(TAG_RELOAD_END) > now) {
            return true;
        }
        if (!canReloadCrossbow(player, hand)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.no_ammo"), true);
            return true;
        }
        int chargeTicks = CrossbowItem.getChargeDuration(stack);
        player.getPersistentData().putLong(TAG_RELOAD_END, now + chargeTicks);
        player.getPersistentData().putInt(TAG_RELOAD_HAND, hand == InteractionHand.OFF_HAND ? 1 : 0);
        // Vanilla picks the "crossbow held in front" arm pose from CrossbowItem#isCharged, so mark
        // the stack charged for the duration of the reload; DndLoaded still gates actual firing.
        CrossbowItem.setCharged(stack, true);
        // Per-stack marker: only this crossbow shows the loading animation, not every crossbow in
        // the hotbar.
        stack.getOrCreateTag().putBoolean(DndWeaponItem.TAG_RELOADING, true);
        player.displayClientMessage(Component.translatable("message.dndclasses.reloading"), true);
        return true;
    }

    /**
     * Loads the weapon once the charge time elapsed, or drops the charge when it was interrupted.
     * <p>
     * The progress lives only in {@link #TAG_RELOAD_END}: a firing attempt
     * ({@link #cancelReload}) or switching the crossbow away ends it, so an interrupted reload has
     * to be started again from zero.
     */
    public static void tickReload(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        long end = tag.getLong(TAG_RELOAD_END);
        if (end <= 0L) {
            return;
        }
        InteractionHand hand = tag.getInt(TAG_RELOAD_HAND) == 1
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (player.isDeadOrDying()
                || !(stack.getItem() instanceof DndWeapon weaponItem)
                || !weaponItem.spec().isCrossbow()) {
            clearReload(player);
            return;
        }
        if (player.level().getGameTime() < end) {
            return;
        }
        clearReload(player);
        completeLoading(player, hand);
    }

    private static void clearReload(Player player) {
        player.getPersistentData().remove(TAG_RELOAD_END);
        player.getPersistentData().remove(TAG_RELOAD_HAND);
    }

    /** Drops an in-progress charge (interrupted animation, firing attempt, item swap, ...). */
    public static void cancelReload(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        CompoundTag tag = player.getPersistentData();
        if (tag.getLong(TAG_RELOAD_END) <= 0L) {
            return;
        }
        InteractionHand hand = tag.getInt(TAG_RELOAD_HAND) == 1
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof DndWeapon weaponItem && weaponItem.spec().isCrossbow()) {
            CrossbowItem.setCharged(stack, false);
            stack.getOrCreateTag().remove(DndWeaponItem.TAG_RELOADING);
        }
        clearReload(player);
    }

    public static boolean completeLoading(Player player, InteractionHand hand) {
        clearReload(player);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.hasTag()) {
            stack.getTag().remove(DndWeaponItem.TAG_RELOADING);
        }
        if (!(stack.getItem() instanceof DndWeapon weaponItem) || !weaponItem.spec().isLoading()) {
            return false;
        }
        if (stack.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED)) {
            return false;
        }
        String ammoId = ammoSuffixFor(weaponItem.spec());
        AmmoResult ammo = consumeAmmo(player, ammoId);
        if (ammo == null) {
            player.displayClientMessage(Component.translatable("message.dndclasses.no_ammo"), true);
            return true;
        }
        stack.getOrCreateTag().putBoolean(DndWeaponItem.TAG_LOADED, true);
        stack.getOrCreateTag().putString(DndWeaponItem.TAG_LOADED_AMMO, ammo.id());
        if (weaponItem.spec().isCrossbow()) {
            CrossbowItem.setCharged(stack, true);
        }
        player.displayClientMessage(Component.translatable("message.dndclasses.loaded", stack.getHoverName()), true);
        return true;
    }

    private static boolean hasAmmo(Player player, String suffix) {
        return findAmmo(player, suffix) != null;
    }

    private static Optional<ItemStack> takeAmmoForShot(Player player, ItemStack weaponStack, DndWeaponSpec spec) {
        if (spec.isLoading()) {
            String ammoId = weaponStack.getOrCreateTag().getString(DndWeaponItem.TAG_LOADED_AMMO);
            if (ammoId.isEmpty()) {
                AmmoResult found = findAmmo(player, ammoSuffixFor(spec));
                if (found == null) {
                    return Optional.empty();
                }
                ammoId = found.id();
            }
            if (!WeaponItems.AMMO.containsKey(ammoId)) {
                return Optional.empty();
            }
            return Optional.of(new ItemStack(WeaponItems.ammo(ammoId).get()));
        }
        AmmoResult ammo = consumeAmmo(player, ammoSuffixFor(spec));
        return ammo == null ? Optional.empty() : Optional.of(ammo.stack());
    }

    private static AmmoResult findAmmo(Player player, String suffix) {
        String needle = "_" + suffix;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (String id : WeaponItems.AMMO.keySet()) {
                if (!id.endsWith(needle)) {
                    continue;
                }
                if (stack.is(WeaponItems.AMMO.get(id).get())) {
                    return new AmmoResult(id, stack, i);
                }
            }
        }
        if (player.getAbilities().instabuild) {
            for (String id : WeaponItems.AMMO.keySet()) {
                if (id.endsWith(needle)) {
                    return new AmmoResult(id, new ItemStack(WeaponItems.AMMO.get(id).get()), -1);
                }
            }
        }
        return null;
    }

    private static AmmoResult consumeAmmo(Player player, String suffix) {
        AmmoResult found = findAmmo(player, suffix);
        if (found == null) {
            return null;
        }
        ItemStack single = found.stack().copy();
        single.setCount(1);
        if (found.slot() >= 0 && !player.getAbilities().instabuild) {
            ItemStack stack = player.getInventory().getItem(found.slot());
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.getInventory().setItem(found.slot(), ItemStack.EMPTY);
            }
        }
        return new AmmoResult(found.id(), single, -1);
    }

    private static String ammoSuffixFor(DndWeaponSpec spec) {
        return switch (spec.type()) {
            case "light_crossbow", "hand_crossbow", "heavy_crossbow" -> "bolt";
            case "blowgun" -> "blowgun_needle";
            case "sling" -> "sling_bullet";
            default -> "arrow";
        };
    }

    private record AmmoResult(String id, ItemStack stack, int slot) {
    }

    public static boolean canUseWeapon(Player player, InteractionHand hand, DndWeaponSpec spec, boolean sendMessage) {
        boolean requiresTwoHands = spec.isTwoHandedOnly() || ("lance".equals(spec.type()) && player.getVehicle() == null);
        if (requiresTwoHands && hand == InteractionHand.MAIN_HAND && !player.getOffhandItem().isEmpty()) {
            if (sendMessage) {
                player.displayClientMessage(Component.translatable("message.dndclasses.two_handed"), true);
            }
            return false;
        }
        if (requiresTwoHands && hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            if (sendMessage) {
                player.displayClientMessage(Component.translatable("message.dndclasses.two_handed"), true);
            }
            return false;
        }
        return true;
    }

    public static int minimumDamage(int damage) {
        return Math.max(1, damage);
    }

    /** Golems, shulkers and similar objects/constructs: orichalcum weapons always crit them. */
    public static boolean isConstruct(LivingEntity entity) {
        return entity instanceof IronGolem || entity instanceof SnowGolem || entity instanceof Shulker
                || entity instanceof ArmorStand;
    }
}
