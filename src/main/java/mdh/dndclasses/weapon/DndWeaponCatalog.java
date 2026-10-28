package mdh.dndclasses.weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static mdh.dndclasses.weapon.AbilityMode.DEXTERITY;
import static mdh.dndclasses.weapon.AbilityMode.STRENGTH;
import static mdh.dndclasses.weapon.DndDamageType.*;
import static mdh.dndclasses.weapon.WeaponProperty.*;

/** All 37 D&amp;D 5e (2014) weapons; ranged launchers exist only in the iron tier. */
public final class DndWeaponCatalog {

    /**
     * Ranged weapons exist only in the mundane (iron) tier: their material difference lives in the
     * ammunition (arrows, bolts, needles, bullets), so silver/orichalcum versions of the launcher
     * itself are obsolete. The dart is excluded on purpose - it is its own ammunition.
     * <p>
     * Declared before {@link #WEAPONS}: the static initialiser runs top-down, so a later field
     * would still be {@code null} while {@link #build()} filters the list.
     */
    private static final Set<String> IRON_ONLY_RANGED = Set.of(
            "light_crossbow", "shortbow", "sling", "blowgun",
            "hand_crossbow", "heavy_crossbow", "longbow", "net");

    public static final List<DndWeaponSpec> WEAPONS = build();

    private DndWeaponCatalog() {
    }

    private static List<DndWeaponSpec> build() {
        List<DndWeaponSpec> out = new ArrayList<>();
        for (DndWeaponMaterial material : DndWeaponMaterial.values()) {
            // simple melee
            out.add(DndWeaponSpec.melee(material, "club", 2, BLUDGEONING, STRENGTH, LIGHT));
            out.add(DndWeaponSpec.melee(material, "dagger", 2, PIERCING, AbilityMode.FINESSE, FINESSE, LIGHT, THROWN));
            out.add(DndWeaponSpec.melee(material, "greatclub", 4, BLUDGEONING, STRENGTH, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "handaxe", 3, SLASHING, STRENGTH, LIGHT, THROWN, AXE_DIG));
            out.add(DndWeaponSpec.melee(material, "javelin", 3, PIERCING, STRENGTH, THROWN));
            out.add(DndWeaponSpec.melee(material, "light_hammer", 2, BLUDGEONING, STRENGTH, LIGHT, THROWN));
            out.add(DndWeaponSpec.melee(material, "mace", 3, BLUDGEONING, STRENGTH));
            out.add(DndWeaponSpec.versatile(material, "quarterstaff", 3, 4, BLUDGEONING, STRENGTH));
            out.add(DndWeaponSpec.melee(material, "sickle", 2, SLASHING, STRENGTH, LIGHT));
            out.add(DndWeaponSpec.versatile(material, "spear", 3, 4, PIERCING, STRENGTH, THROWN));
            // simple ranged
            out.add(DndWeaponSpec.ranged(material, "light_crossbow", 4, PIERCING, 80, 320, LOADING, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "dart", 2, PIERCING, AbilityMode.FINESSE, FINESSE, THROWN));
            out.add(DndWeaponSpec.ranged(material, "shortbow", 3, PIERCING, 80, 320, TWO_HANDED));
            out.add(DndWeaponSpec.ranged(material, "sling", 2, BLUDGEONING, 30, 120));
            // martial melee
            out.add(DndWeaponSpec.versatile(material, "battleaxe", 4, 5, SLASHING, STRENGTH, AXE_DIG));
            out.add(DndWeaponSpec.melee(material, "flail", 4, BLUDGEONING, STRENGTH));
            out.add(DndWeaponSpec.melee(material, "glaive", 5, SLASHING, STRENGTH, HEAVY, REACH, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "greataxe", 6, SLASHING, STRENGTH, HEAVY, TWO_HANDED, AXE_DIG));
            out.add(DndWeaponSpec.melee(material, "greatsword", 7, SLASHING, STRENGTH, HEAVY, TWO_HANDED, SWORD_DIG));
            out.add(DndWeaponSpec.melee(material, "halberd", 5, SLASHING, STRENGTH, HEAVY, REACH, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "lance", 6, PIERCING, STRENGTH, REACH, SPECIAL));
            out.add(DndWeaponSpec.versatile(material, "longsword", 4, 5, SLASHING, STRENGTH, SWORD_DIG));
            out.add(DndWeaponSpec.melee(material, "maul", 7, BLUDGEONING, STRENGTH, HEAVY, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "morningstar", 4, PIERCING, STRENGTH));
            out.add(DndWeaponSpec.melee(material, "pike", 5, PIERCING, STRENGTH, HEAVY, REACH, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "rapier", 4, PIERCING, AbilityMode.FINESSE, FINESSE));
            out.add(DndWeaponSpec.melee(material, "scimitar", 3, SLASHING, AbilityMode.FINESSE, FINESSE, LIGHT, SWORD_DIG));
            out.add(DndWeaponSpec.melee(material, "shortsword", 3, PIERCING, AbilityMode.FINESSE, FINESSE, LIGHT, SWORD_DIG));
            out.add(DndWeaponSpec.versatile(material, "trident", 3, 4, PIERCING, STRENGTH, THROWN));
            out.add(DndWeaponSpec.melee(material, "war_pick", 4, PIERCING, STRENGTH, PICKAXE_DIG));
            out.add(DndWeaponSpec.versatile(material, "warhammer", 4, 5, BLUDGEONING, STRENGTH));
            out.add(DndWeaponSpec.melee(material, "whip", 2, SLASHING, AbilityMode.FINESSE, FINESSE, REACH));
            // martial ranged
            out.add(DndWeaponSpec.ranged(material, "blowgun", 1, PIERCING, 25, 100, LOADING));
            out.add(DndWeaponSpec.ranged(material, "hand_crossbow", 3, PIERCING, 30, 120, LIGHT, LOADING));
            out.add(DndWeaponSpec.ranged(material, "heavy_crossbow", 5, PIERCING, 100, 400, HEAVY, LOADING, TWO_HANDED));
            out.add(DndWeaponSpec.ranged(material, "longbow", 4, PIERCING, 150, 600, HEAVY, TWO_HANDED));
            out.add(DndWeaponSpec.melee(material, "net", 0, BLUDGEONING, DEXTERITY, SPECIAL, THROWN));
        }
        out.removeIf(spec -> spec.material() != DndWeaponMaterial.IRON
                && IRON_ONLY_RANGED.contains(spec.type()));
        return List.copyOf(out);
    }
}
