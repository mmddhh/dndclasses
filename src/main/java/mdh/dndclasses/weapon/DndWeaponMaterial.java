package mdh.dndclasses.weapon;

/** Material of a D&amp;D weapon. Silver counts as a magic weapon; adamantine is far more durable. */
public enum DndWeaponMaterial {
    IRON("iron", 250, false),
    ADAMANTINE("adamantine", 500, false),
    SILVER("silver", 250, true);

    private final String id;
    private final int durability;
    private final boolean magical;

    DndWeaponMaterial(String id, int durability, boolean magical) {
        this.id = id;
        this.durability = durability;
        this.magical = magical;
    }

    public String id() {
        return id;
    }

    public int durability() {
        return durability;
    }

    public boolean magical() {
        return magical;
    }
}
