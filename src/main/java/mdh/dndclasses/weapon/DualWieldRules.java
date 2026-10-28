package mdh.dndclasses.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * D&amp;D two-weapon fighting rules.
 * <ul>
 *   <li>A weapon may only be placed in the offhand when the main hand holds nothing, a non-weapon
 *       item, or a <b>light</b> weapon.</li>
 *   <li>Dual wielding requires a light weapon in <b>both</b> hands.</li>
 *   <li>Attacks then alternate between the hands (the offhand swing is the bonus-action attack).</li>
 * </ul>
 */
public final class DualWieldRules {

    /** After this many ticks without an attack the alternation restarts with the main hand. */
    public static final int RESET_TICKS = 30;

    private static final String TAG_NEXT_OFF = "DndDualWieldNextOff";
    private static final String TAG_LAST = "DndDualWieldLast";

    private DualWieldRules() {
    }

    public static boolean isLightWeapon(ItemStack stack) {
        return stack.getItem() instanceof DndWeapon weapon && weapon.spec().has(WeaponProperty.LIGHT);
    }

    /** Whether {@code offStack} may be placed in the offhand slot at all. */
    public static boolean canHoldInOffHand(Player player, ItemStack offStack) {
        if (!(offStack.getItem() instanceof DndWeapon)) {
            return true;
        }
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof DndWeapon mainWeapon)) {
            return true;
        }
        return mainWeapon.spec().has(WeaponProperty.LIGHT);
    }

    /** Both hands hold a light D&amp;D weapon: attacks alternate between them. */
    public static boolean canDualWield(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main == off) {
            return false;
        }
        return isLightWeapon(main) && isLightWeapon(off);
    }

    /** The hand the next attack should use. */
    public static InteractionHand nextHand(Player player) {
        CompoundTag tag = player.getPersistentData();
        if (!canDualWield(player)) {
            reset(player);
            return InteractionHand.MAIN_HAND;
        }
        long now = player.level().getGameTime();
        long last = tag.getLong(TAG_LAST);
        if (last > 0L && now - last > RESET_TICKS) {
            reset(player);
        }
        return tag.getBoolean(TAG_NEXT_OFF) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Records a performed attack and flips the alternation to the other hand. */
    public static void recordAttack(Player player, InteractionHand hand) {
        CompoundTag tag = player.getPersistentData();
        tag.putLong(TAG_LAST, player.level().getGameTime());
        tag.putBoolean(TAG_NEXT_OFF, hand == InteractionHand.MAIN_HAND);
    }

    public static void reset(Player player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(TAG_NEXT_OFF);
        tag.remove(TAG_LAST);
    }
}
