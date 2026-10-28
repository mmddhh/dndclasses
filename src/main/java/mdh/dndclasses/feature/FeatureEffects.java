package mdh.dndclasses.feature;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reads numeric values out of the effects of a player's currently active features, so combat code
 * can ask "how many extra attacks does this player have" without knowing which feature granted it.
 */
public final class FeatureEffects {

    public static final ResourceLocation EXTRA_ATTACK = new ResourceLocation("dndclasses", "extra_attack");
    public static final ResourceLocation ATTACK_BONUS = new ResourceLocation("dndclasses", "attack_bonus");

    private FeatureEffects() {
    }

    /** Sum of {@code field} over every active effect of the given type (dice use their expectation). */
    public static double total(ServerPlayer player, ResourceLocation type, String field) {
        double total = 0;
        for (FeatureDefinition feature : FeatureManager.activeFeatureList(player)) {
            // Only passive features: an active feature's bonus would otherwise apply permanently
            // instead of while the feature is actually running.
            if (feature.kind() != FeatureDefinition.Kind.PASSIVE) {
                continue;
            }
            for (EffectDefinition effect : feature.effects()) {
                if (!type.equals(effect.type())) {
                    continue;
                }
                JsonObject params = effect.params();
                if (params != null && params.has(field) && !params.get(field).isJsonNull()) {
                    total += Scaling.eval(player, params.get(field));
                }
            }
        }
        return total;
    }

    /** Extra attacks granted by passive features: 0 = one attack, 1 = two attacks, ... */
    public static int extraAttacks(ServerPlayer player) {
        return Math.max(0, (int) Math.round(total(player, EXTRA_ATTACK, "attacks")));
    }

    /**
     * Flat bonus added to every weapon attack. The mod resolves hits without a d20 to-hit roll, so
     * an "attack bonus" is applied as extra damage instead of an accuracy bonus.
     */
    public static double attackBonus(ServerPlayer player) {
        return total(player, ATTACK_BONUS, "amount");
    }
}
