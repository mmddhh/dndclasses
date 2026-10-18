package mdh.dndclasses.spellcasting;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.network.ServerStateSync;
import mdh.dndclasses.spells.IDndSpell;
import mdh.dndclasses.spells.Preparation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Fills the player's spell capability from the data-driven spellcasting table for their class
 * (or subclass). Non-casters get everything zeroed. Slot maxima are applied without refilling
 * current slots, except that newly gained slots at level-up are added; long rest refills fully.
 * Spell lists are only trimmed to their limits, never auto-populated.
 * <p>
 * Prepared casters get their prepared-spell limit scaled by their casting-ability modifier
 * (limit = table value + modifier), except at levels with no slots at all, where it is 0.
 */
public final class SpellcastingManager {

    private SpellcastingManager() {
    }

    public static void refresh(ServerPlayer player) {
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            if (!level.hasCreatedCharacter()) {
                return;
            }
            ResourceLocation owner = pickOwner(level.getclassName(), level.getSubclassKey());
            SpellLevelEntry entry = owner == null ? null : SpellcastingRegistry.get(owner, level.getlevel());
            Preparation preparation = owner == null ? Preparation.NONE : SpellcastingRegistry.getPreparation(owner);
            String ability = owner == null ? "" : SpellcastingRegistry.getAbility(owner);
            player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> apply(player, spell, entry, preparation, ability));
        });
    }

    /** Called on a short rest: refills slots for owners that recover on a short rest (warlock pact magic). */
    public static void onShortRest(ServerPlayer player) {
        boolean[] recovered = {false};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            if (!level.hasCreatedCharacter()) {
                return;
            }
            ResourceLocation owner = pickOwner(level.getclassName(), level.getSubclassKey());
            if (owner == null || !SpellcastingRegistry.isShortRestRecovery(owner)) {
                return;
            }
            player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
                spell.reslots();
                recovered[0] = true;
            });
        });
        if (recovered[0]) {
            ServerStateSync.send(player);
        }
    }

    private static ResourceLocation pickOwner(String className, String subclassRaw) {
        if (className != null && !className.isEmpty()) {
            ResourceLocation classId = ResourceLocation.tryParse("dndclasses:" + className);
            if (classId != null && SpellcastingRegistry.has(classId)) {
                return classId;
            }
        }
        if (subclassRaw != null && !subclassRaw.isEmpty()) {
            ResourceLocation subclassId = ResourceLocation.tryParse(subclassRaw);
            if (subclassId != null && SpellcastingRegistry.has(subclassId)) {
                return subclassId;
            }
        }
        return null;
    }

    private static void apply(ServerPlayer player, IDndSpell spell, SpellLevelEntry entry, Preparation preparation, String ability) {
        if (entry == null) {
            for (int ring = 1; ring <= 9; ring++) {
                spell.setmaxspellslots(ring, 0);
                spell.setspellslots(ring, 0);
            }
            spell.setPreparation(Preparation.NONE);
            spell.setKnownCantripLimit(0);
            spell.setKnownSpellLimit(0);
            spell.setPreparedSpellLimit(0);
            spell.enforceLimits();
            return;
        }

        int[] slots = entry.slots();
        boolean hasSlots = false;
        for (int ring = 1; ring <= 9; ring++) {
            int newMax = ring <= slots.length ? slots[ring - 1] : 0;
            if (newMax > 0) {
                hasSlots = true;
            }
            int oldMax = spell.getmaxspellslots(ring);
            int oldCurrent = spell.getspellslots(ring);
            spell.setmaxspellslots(ring, newMax);
            int target = oldCurrent + Math.max(0, newMax - oldMax);
            spell.setspellslots(ring, Math.min(target, newMax));
        }

        spell.setPreparation(preparation);
        spell.setKnownCantripLimit(entry.knownCantrips());
        spell.setKnownSpellLimit(entry.knownSpells());
        if (preparation == Preparation.PREPARED) {
            int limit = hasSlots ? entry.preparedSpellLimit() + castingModifier(player, ability) : 0;
            spell.setPreparedSpellLimit(limit);
        } else {
            spell.setPreparedSpellLimit(entry.preparedSpellLimit());
        }
        spell.enforceLimits();
    }

    private static int castingModifier(ServerPlayer player, String ability) {
        if (ability == null || ability.isEmpty()) {
            return 0;
        }
        int[] modifier = {0};
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(abilityData -> {
            switch (ability.toLowerCase(Locale.ROOT)) {
                case "int" -> modifier[0] = abilityData.getintmod();
                case "wis" -> modifier[0] = abilityData.getwismod();
                case "cha" -> modifier[0] = abilityData.getchamod();
                default -> modifier[0] = 0;
            }
        });
        return modifier[0];
    }
}
