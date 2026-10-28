package mdh.dndclasses.weapon;

/** Material of a D&amp;D weapon. Silver counts as a magic weapon; orichalcum crits constructs. */
public enum DndWeaponMaterial {
    IRON("iron", 250, false),
    ORICHALCUM("orichalcum", 250, false),
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
