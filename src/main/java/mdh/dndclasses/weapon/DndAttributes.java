package mdh.dndclasses.weapon;

import mdh.dndclasses.ability.Iability;
import mdh.dndclasses.capability.ModCapabilities;
import net.minecraft.world.entity.player.Player;

public final class DndAttributes {

    private DndAttributes() {
    }

    public static int modifier(Player player, AbilityMode mode) {
        return player.getCapability(ModCapabilities.ABILITY_CAPABILITY).map(ability -> switch (mode) {
            case STRENGTH -> ability.getstrmod();
            case DEXTERITY -> ability.getdexmod();
            case CONSTITUTION -> ability.getconmod();
            case FINESSE -> Math.max(ability.getstrmod(), ability.getdexmod());
        }).orElse(0);
    }

    public static int strengthModifier(Player player) {
        return player.getCapability(ModCapabilities.ABILITY_CAPABILITY).map(Iability::getstrmod).orElse(0);
    }

    public static int abilityScore(Player player, String ability) {
        int[] value = {8};
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(data -> {
            switch (ability) {
                case "str" -> value[0] = data.getstr();
                case "dex" -> value[0] = data.getdex();
                case "con" -> value[0] = data.getcon();
                case "int" -> value[0] = data.getint();
                case "wis" -> value[0] = data.getwis();
                case "cha" -> value[0] = data.getcha();
                default -> value[0] = 8;
            }
        });
        return value[0];
    }
}
