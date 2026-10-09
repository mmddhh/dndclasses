package mdh.dndclasses.spellcasting;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.spells.IDndSpell;
import mdh.dndclasses.spells.Preparation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fills the player's spell capability from the data-driven spellcasting table for their class
 * (or subclass). Non-casters get everything zeroed. Slot maxima are applied without refilling
 * current slots, except that newly gained slots at level-up are added; long rest refills fully.
 * Spell lists are only trimmed to their limits, never auto-populated.
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
            player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> apply(spell, entry, preparation));
        });
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

    private static void apply(IDndSpell spell, SpellLevelEntry entry, Preparation preparation) {
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
        for (int ring = 1; ring <= 9; ring++) {
            int newMax = ring <= slots.length ? slots[ring - 1] : 0;
            int oldMax = spell.getmaxspellslots(ring);
            int oldCurrent = spell.getspellslots(ring);
            spell.setmaxspellslots(ring, newMax);
            int target = oldCurrent + Math.max(0, newMax - oldMax);
            spell.setspellslots(ring, Math.min(target, newMax));
        }

        spell.setPreparation(preparation);
        spell.setKnownCantripLimit(entry.knownCantrips());
        spell.setKnownSpellLimit(entry.knownSpells());
        spell.setPreparedSpellLimit(entry.preparedSpellLimit());
        spell.enforceLimits();
    }
}
