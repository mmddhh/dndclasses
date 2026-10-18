package mdh.dndclasses.weapon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class DndCooldowns {
    public static final int ATTACK_COOLDOWN_TICKS = 10;

    private DndCooldowns() {
    }

    public static boolean tryUse(Player player, InteractionHand hand) {
        String key = key(hand);
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(key);
        if (now - last < ATTACK_COOLDOWN_TICKS) {
            return false;
        }
        player.getPersistentData().putLong(key, now);
        return true;
    }

    public static boolean isCoolingDown(Player player, InteractionHand hand) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(key(hand));
        return now - last < ATTACK_COOLDOWN_TICKS;
    }

    private static String key(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? "DndWeaponMainCooldown" : "DndWeaponOffCooldown";
    }
}
