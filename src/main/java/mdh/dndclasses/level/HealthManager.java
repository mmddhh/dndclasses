package mdh.dndclasses.level;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.data.ClassRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Gives the player a D&amp;D hit-point pool: {@code class hit die + CON modifier}, using the
 * expectation of the die (no randomness, consistent with {@code Scaling}).
 * <ul>
 *   <li>Level 1: {@code dieSize + conMod}.</li>
 *   <li>Each level after: {@code (dieSize + 1) / 2 + conMod} (the die's expectation).</li>
 * </ul>
 * The total is rounded to an int and written to the max-health base value; current health is raised
 * by the gained amount on level-up and clamped otherwise, so damage taken is preserved.
 */
public final class HealthManager {

    private HealthManager() {
    }

    /**
     * Computed D&amp;D max HP for this player's class / level / CON (rounded). Returns {@code 0}
     * when the player has no character or an unknown class (callers should then leave vanilla HP).
     */
    public static double getMaxHp(ServerPlayer player) {
        double[] result = {0};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            if (!level.hasCreatedCharacter()) {
                return;
            }
            int dieSize = ClassRegistry.getHitDieValue(level.getclassName());
            if (dieSize <= 0) {
                return;
            }
            int playerLevel = Math.max(1, level.getlevel());
            int conMod = conModifier(player);
            double expected = dieSize + conMod + (playerLevel - 1) * ((dieSize + 1) / 2.0 + conMod);
            result[0] = Math.max(1.0, (double) Math.round(expected));
        });
        return result[0];
    }

    public static void apply(ServerPlayer player) {
        double target = getMaxHp(player);
        if (target <= 0) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double old = maxHealth.getBaseValue();
        if (old == target) {
            return;
        }

        double delta = target - old;
        maxHealth.setBaseValue(target);
        float newMax = player.getMaxHealth();
        if (delta > 0) {
            player.setHealth((float) Math.min(player.getHealth() + delta, newMax));
        } else {
            player.setHealth((float) Math.min(player.getHealth(), newMax));
        }
    }

    private static int conModifier(ServerPlayer player) {
        int[] modifier = {0};
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(ability -> modifier[0] = ability.getconmod());
        return modifier[0];
    }
}
