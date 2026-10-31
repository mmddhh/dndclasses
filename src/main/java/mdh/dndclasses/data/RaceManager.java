package mdh.dndclasses.data;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.race.Irace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Applies the mechanical parts of a race that are implemented so far: ASI (at creation) and speed. */
public final class RaceManager {

    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("7d2f6a5e-3c1b-4e8a-9f60-2b9d4c7e1a53");

    private RaceManager() {
    }

    public static boolean isValidSelection(String race, String subrace, List<String> asiChoices) {
        if (!RaceData.isValidRace(race) || !RaceData.isValidSubrace(race, subrace)) {
            return false;
        }
        int required = RaceData.asiChoiceCount(race, subrace);
        if (required == 0) {
            return asiChoices == null || asiChoices.isEmpty();
        }
        if (asiChoices == null || asiChoices.size() != required) {
            return false;
        }
        List<String> seen = new ArrayList<>();
        for (String ability : asiChoices) {
            if (!RaceData.ABILITIES.contains(ability) || seen.contains(ability)) {
                return false;
            }
            seen.add(ability);
        }
        return true;
    }

    /** Stores race/subrace/features on the capability and applies the speed modifier. */
    public static void setRace(ServerPlayer player, String race, String subrace, List<String> asiChoices) {
        List<String> traits = new ArrayList<>(RaceData.traits(race, subrace));
        if (asiChoices != null && !asiChoices.isEmpty()) {
            for (String ability : asiChoices) {
                traits.add("asi_choice_" + ability);
            }
        }
        player.getCapability(ModCapabilities.RACE_CAPABILITY).ifPresent(data -> {
            data.setRaceName(race);
            data.setSubraceName(subrace);
            data.setFeatures(traits);
        });
        applySpeed(player);
    }

    /** Re-applies the speed modifier from the stored race (called on refresh/login). */
    public static void applySpeed(ServerPlayer player) {
        String race = "";
        String subrace = "";
        Irace data = player.getCapability(ModCapabilities.RACE_CAPABILITY).orElse(null);
        if (data != null) {
            race = data.getRaceName();
            subrace = data.getSubraceName();
        }
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(SPEED_MODIFIER_ID);
        if (!RaceData.isValidSubrace(race, subrace)) {
            return;
        }
        int feet = RaceData.speedFeet(race, subrace);
        if (feet == 30) {
            return;
        }
        double amount = 0.1D * (feet / 30.0D - 1.0D);
        attribute.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID,
                "DND racial speed", amount, AttributeModifier.Operation.ADDITION));
    }

    /** Fixed + flexible racial bonuses for the given selection. */
    public static Map<String, Integer> bonuses(String race, String subrace, List<String> asiChoices) {
        return RaceData.totalBonuses(race, subrace, asiChoices);
    }
}
