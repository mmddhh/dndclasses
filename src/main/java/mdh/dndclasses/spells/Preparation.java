package mdh.dndclasses.spells;

/** How a class learns leveled spells. */
public enum Preparation {
    /** Non-caster: cannot cast leveled spells. */
    NONE,
    /** Known caster (bard/sorcerer/warlock/ranger): knownSpells are castable directly. */
    KNOWN,
    /** Prepared caster (cleric/druid/paladin/wizard): only preparedSpells are castable. */
    PREPARED
}
