package mdh.dndclasses.saves;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.data.ClassRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

/** Ability/saving-throw helpers shared by the save-QTE system and the combat layer. */
public final class Saves {

    private Saves() {
    }

    /** Total saving-throw bonus: ability modifier + proficiency (when the class is proficient). */
    public static int getSaveBonus(ServerPlayer player, String ability) {
        if (ability == null || ability.isEmpty()) {
            return 0;
        }
        String key = ability.toLowerCase(Locale.ROOT);
        int modifier = abilityModifier(player, key);
        int proficiency = proficiencyBonus(player);
        return modifier + (isProficient(player, key) ? proficiency : 0);
    }

    /** Ability modifier for str/dex/con/int/wis/cha; 0 when unknown. */
    public static int abilityModifier(ServerPlayer player, String ability) {
        int[] value = {0};
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(data -> {
            switch (ability) {
                case "str" -> value[0] = data.getstrmod();
                case "dex" -> value[0] = data.getdexmod();
                case "con" -> value[0] = data.getconmod();
                case "int" -> value[0] = data.getintmod();
                case "wis" -> value[0] = data.getwismod();
                case "cha" -> value[0] = data.getchamod();
                default -> value[0] = 0;
            }
        });
        return value[0];
    }

    private static int proficiencyBonus(ServerPlayer player) {
        int[] value = {2};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> value[0] = level.getproficiencybonus());
        return value[0];
    }

    private static boolean isProficient(ServerPlayer player, String ability) {
        boolean[] result = {false};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            List<String> saves = ClassRegistry.getSaveProficiencies(level.getclassName());
            result[0] = saves.contains(ability);
        });
        return result[0];
    }
}
