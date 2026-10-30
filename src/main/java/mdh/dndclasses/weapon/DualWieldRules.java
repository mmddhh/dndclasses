package mdh.dndclasses.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * D&amp;D two-weapon fighting rules.
 * <ul>
 *   <li>An offhand weapon may only be held when both hands hold <b>light</b> weapons.</li>
 *   <li>Dual wielding then alternates the attacking hand (the offhand swing is the bonus-action
 *       attack).</li>
 *   <li>Dual wielding halves the attack interval (see {@link DndCooldowns}) so the vanilla charge
 *       indicator and the alternation stay in sync.</li>
 * </ul>
 */
public final class DualWieldRules {

    private static final String TAG_NEXT_OFF = "DndDualWieldNextOff";
    private static final UUID DUAL_WIELD_SPEED_ID = UUID.fromString("3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f");

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

    /**
     * Keeps the player's attack speed in sync with the dual-wield state: {@code x2} while both
     * hands hold light weapons, so the vanilla charge indicator reaches full at 5 ticks and the
     * "attack only when fully charged" rule matches the 5 tick alternation.
     */
    public static void updateAttackSpeed(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute == null) {
            return;
        }
        boolean dual = canDualWield(player);
        boolean has = attribute.getModifier(DUAL_WIELD_SPEED_ID) != null;
        if (dual && !has) {
            attribute.addTransientModifier(new AttributeModifier(DUAL_WIELD_SPEED_ID,
                    "DND dual wielding attack speed", 1.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!dual && has) {
            attribute.removeModifier(DUAL_WIELD_SPEED_ID);
        }
    }
}
