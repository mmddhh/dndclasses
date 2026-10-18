package mdh.dndclasses.weapon;

/** The 13 Damage Types of D&amp;D 5e (2014). */
public enum DndDamageType {
    ACID("acid"),
    BLUDGEONING("bludgeoning"),
    COLD("cold"),
    FIRE("fire"),
    FORCE("force"),
    LIGHTNING("lightning"),
    NECROTIC("necrotic"),
    PIERCING("piercing"),
    POISON("poison"),
    PSYCHIC("psychic"),
    RADIANT("radiant"),
    SLASHING("slashing"),
    THUNDER("thunder");

    private final String id;

    DndDamageType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
