package mdh.dndclasses.armor;

import mdh.dndclasses.ore.OreItems;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/** Armor material: durability, repair ingredient and the special traits. */
public enum ArmorMaterialType {
    IRON("iron", 250, () -> Ingredient.of(Items.IRON_INGOT), false, false),
    ORICHALCUM("orichalcum", 250, () -> Ingredient.of(OreItems.ORICHALCUM_INGOT.get()), true, false),
    MITHRIL("mithril", 250, () -> Ingredient.of(OreItems.MITHRIL_INGOT.get()), false, true);

    private final String id;
    private final int durability;
    private final Supplier<Ingredient> repair;
    private final boolean critImmune;
    private final boolean mithril;

    ArmorMaterialType(String id, int durability, Supplier<Ingredient> repair, boolean critImmune, boolean mithril) {
        this.id = id;
        this.durability = durability;
        this.repair = repair;
        this.critImmune = critImmune;
        this.mithril = mithril;
    }

    public String id() {
        return id;
    }

    public int durability() {
        return durability;
    }

    public Ingredient repairIngredient() {
        return repair.get();
    }

    /** Orichalcum armor: critical hits against the wearer become normal hits. */
    public boolean critImmune() {
        return critImmune;
    }

    /** Mithril: ignores the strength requirement and is lighter (no stealth penalty, less stamina/roll cost). */
    public boolean mithril() {
        return mithril;
    }

    public DndArmorMaterial material() {
        return DndArmorMaterial.of(this);
    }
}
