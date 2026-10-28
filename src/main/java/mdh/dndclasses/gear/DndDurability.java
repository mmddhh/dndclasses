package mdh.dndclasses.gear;

import mdh.dndclasses.armor.DndArmorItem;
import mdh.dndclasses.weapon.DndWeapon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Shared "never disappears, becomes broken instead" durability helpers. */
public final class DndDurability {

    public static final String BROKEN_TAG = "DndBroken";

    private DndDurability() {
    }

    public static boolean isDndGear(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() instanceof DndWeapon || stack.getItem() instanceof DndArmorItem);
    }

    public static boolean isBroken(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getOrCreateTag().getBoolean(BROKEN_TAG);
    }

    public static void setBroken(ItemStack stack, boolean broken) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (broken) {
            tag.putBoolean(BROKEN_TAG, true);
        } else {
            tag.remove(BROKEN_TAG);
        }
    }
}
