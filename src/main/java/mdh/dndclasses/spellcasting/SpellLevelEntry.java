package mdh.dndclasses.spellcasting;

/** One row of a spellcasting table: slots per ring 1-9 plus known/cantrip/prepared limits. */
public record SpellLevelEntry(int level, int[] slots, int knownSpells, int knownCantrips, int preparedSpellLimit) {
}
