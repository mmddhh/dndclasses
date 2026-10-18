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
            case FINESSE -> Math.max(ability.getstrmod(), ability.getdexmod());
        }).orElse(0);
    }

    public static int strengthModifier(Player player) {
        return player.getCapability(ModCapabilities.ABILITY_CAPABILITY).map(Iability::getstrmod).orElse(0);
    }
}
