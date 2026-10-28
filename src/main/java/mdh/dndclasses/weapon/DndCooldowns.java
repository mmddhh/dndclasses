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

    public static final int ATTACK_COOLDOWN_TICKS = 10;
    /** Dual wielding attacks twice as fast (one main-hand + one offhand attack per 10 ticks). */
    public static final int DUAL_WIELD_COOLDOWN_TICKS = 5;

    private static final String TAG_LAST_SWING = "DndWeaponSwingCooldown";

    private DndCooldowns() {
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
