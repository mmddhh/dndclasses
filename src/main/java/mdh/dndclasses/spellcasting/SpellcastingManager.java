package mdh.dndclasses.spellcasting;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.spells.IDndSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fills the player's spell capability from the data-driven spellcasting table for their class
 * (or subclass). Non-casters get everything zeroed. Slot maxima are applied without refilling
 * current slots, except that newly gained slots at level-up are added; long rest refills fully.
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
            player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> apply(spell, entry));
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

    private static void apply(IDndSpell spell, SpellLevelEntry entry) {
        if (entry == null) {
            for (int ring = 1; ring <= 9; ring++) {
                spell.setmaxspellslots(ring, 0);
                spell.setspellslots(ring, 0);
            }
            spell.setknowspells(0);
            spell.setknowcantrips(0);
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
        spell.setknowspells(entry.knownSpells());
        spell.setknowcantrips(entry.knownCantrips());
    }
}
