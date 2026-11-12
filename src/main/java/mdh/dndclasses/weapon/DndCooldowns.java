package mdh.dndclasses.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Single, hand-agnostic attack cooldown. Alternating hands never shortens the interval: dual
 * wielding halves it instead (see {@link DualWieldRules}), which is the only source of extra
 * attacks from two-weapon fighting.
 */
public final class DndCooldowns {

    public static final int ATTACK_COOLDOWN_TICKS = 20;
    /** Dual wielding attacks twice as fast (one main-hand + one offhand attack per 20 ticks). */
    public static final int DUAL_WIELD_COOLDOWN_TICKS = 10;

    private static final String TAG_LAST_SWING = "DndWeaponSwingCooldown";
    private static final String TAG_RECENT_ATTACK = "DndRecentAttackTick";
    private static final String TAG_THROW_WINDOW = "DndThrowWindowTick";
    private static final String TAG_OFFHAND_THROW = "DndOffhandThrowTick";

    private DndCooldowns() {
    }

    /**
     * Attack-speed attribute addition that makes the vanilla charge indicator reach full exactly
     * when the attack interval elapses (base player attack speed is {@code 4.0}).
     */
    public static double attackSpeedModifier() {
        return 20.0D / ATTACK_COOLDOWN_TICKS - 4.0D;
    }

    public static boolean tryUse(Player player, InteractionHand hand) {
        CompoundTag tag = player.getPersistentData();
        long now = player.level().getGameTime();
        long last = tag.getLong(TAG_LAST_SWING);
        if (now - last < interval(player)) {
            return false;
        }
        tag.putLong(TAG_LAST_SWING, now);
        return true;
    }

    public static boolean isCoolingDown(Player player, InteractionHand hand) {
        long now = player.level().getGameTime();
        return now - player.getPersistentData().getLong(TAG_LAST_SWING) < interval(player);
    }

    /** Records that a melee attack input happened on this tick (client and server). */
    public static void markAttack(Player player) {
        player.getPersistentData().putLong(TAG_RECENT_ATTACK, player.level().getGameTime());
    }

    /** True when a melee attack input happened within {@code ticks} ticks. */
    public static boolean isRecentAttack(Player player, int ticks) {
        long now = player.level().getGameTime();
        return now - player.getPersistentData().getLong(TAG_RECENT_ATTACK) < ticks;
    }

    /** Marks that a right click is being handled as an offhand throw right now. */
    public static void markThrowWindow(Player player) {
        player.getPersistentData().putLong(TAG_THROW_WINDOW, player.level().getGameTime());
    }

    /** True while an offhand throw was just handled; used to hide stray main-hand swings. */
    public static boolean isThrowWindow(Player player, int ticks) {
        long now = player.level().getGameTime();
        return now - player.getPersistentData().getLong(TAG_THROW_WINDOW) < ticks;
    }

    public static void markOffhandThrow(Player player) {
        player.getPersistentData().putLong(TAG_OFFHAND_THROW, player.level().getGameTime());
    }

    /** Prevents the main hand from being thrown right after an offhand throw emptied the offhand. */
    public static boolean isRecentOffhandThrow(Player player, int ticks) {
        long now = player.level().getGameTime();
        return now - player.getPersistentData().getLong(TAG_OFFHAND_THROW) < ticks;
    }

    private static int interval(Player player) {
        return DualWieldRules.canDualWield(player)
                ? DUAL_WIELD_COOLDOWN_TICKS
                : ATTACK_COOLDOWN_TICKS;
    }
}
