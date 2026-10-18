package mdh.dndclasses.weapon;

import mdh.dndclasses.weapon.ModDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class DndCombat {

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
        int damage;
        if (stack.getItem() instanceof DndWeaponItem weaponItem) {
            DndWeaponSpec spec = weaponItem.spec();
            if (!canUseWeapon(player, hand, spec, true)) {
                return true;
            }
            if (rangedAsMelee && spec.isRanged()) {
                damage = minimumDamage(2 + DndAttributes.strengthModifier(player));
            } else {
                boolean offhandEmpty = player.getOffhandItem().isEmpty();
                damage = minimumDamage(spec.damage(offhandEmpty) + DndAttributes.modifier(player, spec.abilityMode()));
                damageType = spec.damageType();
            }
            stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
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

        livingTarget.hurt(ModDamageTypes.source(player.level(), damageType, player, player), damage);
        player.swing(hand, true);
        player.resetAttackStrengthTicker();
        return true;
    }

    public static boolean throwWeapon(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeaponItem weaponItem) || !weaponItem.spec().isThrown()) {
            return false;
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
        if (player.level().isClientSide) {
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DndWeaponItem weaponItem) || !weaponItem.spec().isRanged()) {
            return false;
        }
        DndWeaponSpec spec = weaponItem.spec();
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

        ThrownWeaponEntity entity = new ThrownWeaponEntity(player.level(), player, ammo.get(), spec);
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.6F, 0.5F);
        player.level().addFreshEntity(entity);
        if (spec.isLoading()) {
            stack.getOrCreateTag().putBoolean(DndWeaponItem.TAG_LOADED, false);
            stack.getOrCreateTag().remove(DndWeaponItem.TAG_LOADED_AMMO);
        }
        player.swing(hand, true);
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
        if (!(stack.getItem() instanceof DndWeaponItem weaponItem) || !weaponItem.spec().isLoading()) {
            return false;
        }
        if (stack.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED)) {
            return false;
        }
        String ammoId = ammoIdFor(weaponItem.spec());
        Optional<ItemStack> ammo = consumeAmmo(player, ammoId);
        if (ammo.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dndclasses.no_ammo"), true);
            return true;
        }
        stack.getOrCreateTag().putBoolean(DndWeaponItem.TAG_LOADED, true);
        stack.getOrCreateTag().putString(DndWeaponItem.TAG_LOADED_AMMO, ammoId);
        player.displayClientMessage(Component.translatable("message.dndclasses.loaded", stack.getHoverName()), true);
        return true;
    }

    private static Optional<ItemStack> takeAmmoForShot(Player player, ItemStack weaponStack, DndWeaponSpec spec) {
        if (spec.isLoading()) {
            String ammoId = weaponStack.getOrCreateTag().getString(DndWeaponItem.TAG_LOADED_AMMO);
            if (ammoId.isEmpty()) {
                ammoId = ammoIdFor(spec);
            }
            return Optional.of(new ItemStack(WeaponItems.ammo(ammoId).get()));
        }
        return consumeAmmo(player, ammoIdFor(spec));
    }

    private static Optional<ItemStack> consumeAmmo(Player player, String ammoId) {
        Item ammoItem = WeaponItems.ammo(ammoId).get();
        if (player.getAbilities().instabuild) {
            return Optional.of(new ItemStack(ammoItem));
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ammoItem)) {
                ItemStack ammo = stack.copy();
                ammo.setCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return Optional.of(ammo);
            }
        }
        return Optional.empty();
    }

    private static String ammoIdFor(DndWeaponSpec spec) {
        String suffix = switch (spec.type()) {
            case "light_crossbow", "hand_crossbow", "heavy_crossbow" -> "bolt";
            case "blowgun" -> "blowgun_needle";
            case "sling" -> "sling_bullet";
            default -> "arrow";
        };
        return spec.material().id() + "_" + suffix;
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
}
