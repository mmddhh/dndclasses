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

/**
 * Race mechanics implemented so far: a pool of free ability points (distributed by the player in
 * the creation UI) and the racial speed modifier. Traits are stored as ids for later implementation.
 */
public final class RaceManager {

    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("7d2f6a5e-3c1b-4e8a-9f60-2b9d4c7e1a53");

    private RaceManager() {
    }

    /** Validates that the assigned values are exactly the race's bonus multiset, on distinct abilities. */
    public static boolean isValidSelection(String race, String subrace, Map<String, Integer> bonuses) {
        if (!RaceData.isValidRace(race) || !RaceData.isValidSubrace(race, subrace)) {
            return false;
        }
        List<Integer> actual = new ArrayList<>();
        if (bonuses != null) {
            for (Map.Entry<String, Integer> entry : bonuses.entrySet()) {
                if (!RaceData.ABILITIES.contains(entry.getKey()) || entry.getValue() == null
                        || entry.getValue() <= 0) {
                    return false;
                }
                actual.add(entry.getValue());
            }
        }
        actual.sort(java.util.Comparator.reverseOrder());
        RaceData.Subrace entry = RaceData.getSubrace(race, subrace);
        return entry != null && entry.asiPattern().equals(actual);
    }

    /** Stores race/subrace/traits on the capability and applies the speed modifier. */
    public static void setRace(ServerPlayer player, String race, String subrace) {
        List<String> traits = new ArrayList<>(RaceData.traits(race, subrace));
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
}
