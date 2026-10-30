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

    private static int interval(Player player) {
        return DualWieldRules.canDualWield(player)
                ? DUAL_WIELD_COOLDOWN_TICKS
                : ATTACK_COOLDOWN_TICKS;
    }
}
