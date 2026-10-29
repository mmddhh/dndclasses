package mdh.dndclasses.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * D&amp;D two-weapon fighting rules.
 * <ul>
 *   <li>An offhand weapon may only be held when both hands hold <b>light</b> weapons.</li>
 *   <li>Dual wielding then alternates the attacking hand (the offhand swing is the bonus-action
 *       attack).</li>
 * </ul>
 */
public final class DualWieldRules {

    private static final String TAG_NEXT_OFF = "DndDualWieldNextOff";

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
        // The offhand weapon itself must always be light.
        if (!isLightWeapon(offStack)) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof DndWeapon mainWeapon)) {
            // Empty or non-weapon main hand: holding the light weapon is fine, dual wielding simply
            // does not engage until a light weapon is equipped in the main hand as well.
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
        // No idle timeout: every attack alternates hands, so a slow clicker still gets the
        // offhand (bonus) attack on the second swing.
        return tag.getBoolean(TAG_NEXT_OFF) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Records a performed attack and flips the alternation to the other hand. */
    public static void recordAttack(Player player, InteractionHand hand) {
        player.getPersistentData().putBoolean(TAG_NEXT_OFF, hand == InteractionHand.MAIN_HAND);
    }

    public static void reset(Player player) {
        player.getPersistentData().remove(TAG_NEXT_OFF);
    }
}
