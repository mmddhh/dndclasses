package mdh.dndclasses.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.level.Ilevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Evaluates declarative numeric values from feature/effect JSON.
 * <p>
 * This mod never rolls dice: a dice spec like {@code "1d10"} is converted to its expectation
 * {@code N * (M + 1) / 2}. A value is either a bare number, a bare dice string, or a structured
 * object:
 * <pre>{ "dice": "1d10", "flat": 0, "per_level": 1, "per_prof": 0,
 *        "ability": "con", "ability_scale": 0 }</pre>
 * Total = dice + flat + per_level * level + per_prof * proficiency + ability_scale * abilityMod.
 */
public final class Scaling {

    private Scaling() {
    }

    /** Evaluates to a double (expected value). Never random. */
    public static double eval(ServerPlayer player, JsonElement node) {
        if (node == null || node.isJsonNull()) {
            return 0;
        }
        if (node.isJsonPrimitive()) {
            return primitiveValue(node.getAsJsonPrimitive());
        }
        if (!node.isJsonObject()) {
            return 0;
        }
        JsonObject object = node.getAsJsonObject();
        double total = 0;
        if (has(object, "dice")) {
            total += diceExpectation(asString(object.get("dice")));
        }
        if (has(object, "flat")) {
            total += object.get("flat").getAsDouble();
        }
        if (has(object, "per_level")) {
            total += object.get("per_level").getAsDouble() * level(player);
        }
        if (has(object, "per_prof")) {
            total += object.get("per_prof").getAsDouble() * proficiency(player);
        }
        if (has(object, "ability") && has(object, "ability_scale")) {
            total += object.get("ability_scale").getAsDouble()
                    * abilityModifier(player, asString(object.get("ability")));
        }
        return total;
    }

    /** Rounded expected value. */
    public static int evalInt(ServerPlayer player, JsonElement node) {
        return (int) Math.round(eval(player, node));
    }

    /** Expectation of an "NdM" (or "dM") dice spec; a plain number is returned as-is. */
    public static double diceExpectation(String spec) {
        if (spec == null) {
            return 0;
        }
        String s = spec.trim().toLowerCase(Locale.ROOT);
        int d = s.indexOf('d');
        if (d < 0) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        int count = 1;
        String countPart = s.substring(0, d).trim();
        if (!countPart.isEmpty()) {
            try {
                count = Integer.parseInt(countPart);
            } catch (NumberFormatException ignored) {
                count = 1;
            }
        }
        int sides;
        try {
            sides = Integer.parseInt(s.substring(d + 1).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
        if (count <= 0 || sides <= 0) {
            return 0;
        }
        return count * (sides + 1) / 2.0;
    }

    private static double primitiveValue(JsonPrimitive primitive) {
        if (primitive.isNumber()) {
            return primitive.getAsDouble();
        }
        if (primitive.isString()) {
            return diceExpectation(primitive.getAsString());
        }
        return 0;
    }

    private static int level(ServerPlayer player) {
        int[] value = {0};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent((Ilevel level) -> value[0] = level.getlevel());
        return value[0];
    }

    private static int proficiency(ServerPlayer player) {
        int[] value = {0};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY)
                .ifPresent((Ilevel level) -> value[0] = level.getproficiencybonus());
        return value[0];
    }

    private static int abilityModifier(ServerPlayer player, String ability) {
        if (ability == null || ability.isEmpty()) {
            return 0;
        }
        int[] value = {0};
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(abilityData -> {
            switch (ability.toLowerCase(Locale.ROOT)) {
                case "str" -> value[0] = abilityData.getstrmod();
                case "dex" -> value[0] = abilityData.getdexmod();
                case "con" -> value[0] = abilityData.getconmod();
                case "int" -> value[0] = abilityData.getintmod();
                case "wis" -> value[0] = abilityData.getwismod();
                case "cha" -> value[0] = abilityData.getchamod();
                default -> value[0] = 0;
            }
        });
        return value[0];
    }

    private static boolean has(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull();
    }

    private static String asString(JsonElement element) {
        return element.isJsonPrimitive() ? element.getAsString() : "";
    }
}
