package mdh.dndclasses.weapon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Set;

/** Vanilla melee weapons/tools that the D&amp;D rules disallow (crafting disabled + inert when held). */
public final class VanillaEquipmentRules {
    private static final Set<Item> FORBIDDEN = Set.of(
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE,
            Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE,
            Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL,
            Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE,
            Items.BOW, Items.CROSSBOW, Items.TRIDENT
    );

    private static final Set<Item> ALLOWED_TOOLS = Set.of(
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
            Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL,
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE
    );

    private VanillaEquipmentRules() {
    }

    public static boolean isForbidden(Item item) {
        return FORBIDDEN.contains(item);
    }

    public static boolean isAllowedTool(Item item) {
        return ALLOWED_TOOLS.contains(item);
    }

    public static DndDamageType toolDamageType(Item item) {
        if (item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.IRON_PICKAXE) {
            return DndDamageType.PIERCING;
        }
        if (item == Items.WOODEN_HOE || item == Items.STONE_HOE || item == Items.IRON_HOE) {
            return DndDamageType.SLASHING;
        }
        return DndDamageType.BLUDGEONING;
    }
}
