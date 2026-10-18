package mdh.dndclasses.weapon;

import java.util.EnumSet;
import java.util.Set;

/**
 * Static definition of one D&amp;D weapon (a material + weapon type).
 * Damage is stored as a floored integer (the average of the PHB dice, rounded down).
 */
public record DndWeaponSpec(
        String id,
        String type,
        DndWeaponMaterial material,
        int oneHandDamage,
        int twoHandDamage,
        DndDamageType damageType,
        AbilityMode abilityMode,
        Set<WeaponProperty> properties,
        int range,
        int longRange
) {
    public DndWeaponSpec {
        properties = Set.copyOf(properties);
    }

    public boolean has(WeaponProperty property) {
        return properties.contains(property);
    }

    public boolean isTwoHandedOnly() {
        return has(WeaponProperty.TWO_HANDED);
    }

    public boolean isVersatile() {
        return has(WeaponProperty.VERSATILE);
    }

    public boolean isThrown() {
        return has(WeaponProperty.THROWN);
    }

    public boolean isRanged() {
        return has(WeaponProperty.AMMUNITION);
    }

    public boolean isLoading() {
        return has(WeaponProperty.LOADING);
    }

    /** Silver weapons count as magic weapons. */
    public boolean magical() {
        return material.magical();
    }

    public int durability() {
        return material.durability();
    }

    public double reach() {
        return has(WeaponProperty.REACH) ? 5.0D : 3.0D;
    }

    /** Floored integer damage; uses the two-handed value for versatile weapons with an empty offhand. */
    public int damage(boolean offhandEmpty) {
        if (isVersatile() && offhandEmpty) {
            return twoHandDamage;
        }
        return oneHandDamage;
    }

    static DndWeaponSpec melee(DndWeaponMaterial material, String type, int damage, DndDamageType damageType, AbilityMode abilityMode, WeaponProperty... properties) {
        return new DndWeaponSpec(material.id() + "_" + type, type, material, damage, damage, damageType, abilityMode, set(properties), 0, 0);
    }

    static DndWeaponSpec versatile(DndWeaponMaterial material, String type, int oneHand, int twoHand, DndDamageType damageType, AbilityMode abilityMode, WeaponProperty... properties) {
        EnumSet<WeaponProperty> set = set(properties);
        set.add(WeaponProperty.VERSATILE);
        return new DndWeaponSpec(material.id() + "_" + type, type, material, oneHand, twoHand, damageType, abilityMode, set, 0, 0);
    }

    static DndWeaponSpec ranged(DndWeaponMaterial material, String type, int damage, DndDamageType damageType, int range, int longRange, WeaponProperty... properties) {
        EnumSet<WeaponProperty> set = set(properties);
        set.add(WeaponProperty.AMMUNITION);
        return new DndWeaponSpec(material.id() + "_" + type, type, material, damage, damage, damageType, AbilityMode.DEXTERITY, set, range, longRange);
    }

    private static EnumSet<WeaponProperty> set(WeaponProperty... properties) {
        EnumSet<WeaponProperty> set = EnumSet.noneOf(WeaponProperty.class);
        for (WeaponProperty property : properties) {
            set.add(property);
        }
        return set;
    }
}
