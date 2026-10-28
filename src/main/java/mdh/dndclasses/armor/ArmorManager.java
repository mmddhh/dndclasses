package mdh.dndclasses.armor;

import mdh.dndclasses.gear.DndDurability;
import mdh.dndclasses.weapon.DndAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Full-set armor rules: only a complete matching set grants AC. Light armor adds the full DEX
 * modifier, medium caps it at +2, heavy ignores it. Orichalcum sets are crit-immune; mithril is
 * lighter and ignores strength requirements.
 */
public final class ArmorManager {

    private static final UUID STRENGTH_PENALTY = UUID.fromString("c3e1a2b4-5d6f-4a8b-9c0d-1e2f3a4b5c6d");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public record WornSet(ArmorType type, ArmorMaterialType material) {
    }

    private ArmorManager() {
    }

    public static WornSet getWornSet(Player player) {
        ArmorType foundType = null;
        ArmorMaterialType foundMaterial = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (DndDurability.isBroken(stack) || !(stack.getItem() instanceof DndArmorItem item)) {
                return null;
            }
            if (foundType == null) {
                foundType = item.armorType();
                foundMaterial = item.materialType();
            } else if (foundType != item.armorType() || foundMaterial != item.materialType()) {
                return null;
            }
        }
        return foundType == null ? null : new WornSet(foundType, foundMaterial);
    }

    public static int getAc(Player player) {
        int dex = DndAttributes.modifier(player, mdh.dndclasses.weapon.AbilityMode.DEXTERITY);
        WornSet set = getWornSet(player);
        if (set == null) {
            return 10 + dex;
        }
        return switch (set.type().category()) {
            case LIGHT -> set.type().baseAc() + dex;
            case MEDIUM -> set.type().baseAc() + Math.min(dex, 2);
            case HEAVY -> set.type().baseAc();
        };
    }

    public static double getStaminaMultiplier(Player player) {
        WornSet set = getWornSet(player);
        if (set == null) {
            return 1.0D;
        }
        double bonus = (set.type().baseAc() - 10) * 0.05D;
        if (set.material().mithril()) {
            bonus *= 0.5D;
        }
        return 1.0D + bonus;
    }

    public static double getRollMultiplier(Player player) {
        WornSet set = getWornSet(player);
        if (set == null || set.material().mithril()) {
            return 1.0D;
        }
        return switch (set.type().category()) {
            case HEAVY -> 0.5D;
            case MEDIUM -> 0.8D;
            default -> 1.0D;
        };
    }

    public static boolean isCritImmune(Player player) {
        WornSet set = getWornSet(player);
        return set != null && set.material().critImmune();
    }

    public static boolean hasStrengthPenalty(Player player) {
        WornSet set = getWornSet(player);
        if (set == null || set.type().category() != ArmorCategory.HEAVY || set.material().mithril()) {
            return false;
        }
        int requirement = set.type().strengthRequirement();
        return requirement > 0 && DndAttributes.abilityScore(player, "str") < requirement;
    }

    /** Recomputes the flat movement penalty from unmet heavy-armor strength requirements. */
    public static void update(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(STRENGTH_PENALTY);
        if (hasStrengthPenalty(player)) {
            speed.addTransientModifier(new AttributeModifier(STRENGTH_PENALTY, "dnd armor strength",
                    -0.02D, AttributeModifier.Operation.ADDITION));
        }
    }
}
