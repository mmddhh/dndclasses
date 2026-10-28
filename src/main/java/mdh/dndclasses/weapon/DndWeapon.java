package mdh.dndclasses.weapon;

/** Common view of D&amp;D weapons implemented by the generic weapon item and the crossbow subclass. */
public interface DndWeapon {

    DndWeaponSpec spec();

    static DndWeaponSpec specOf(Object item) {
        return item instanceof DndWeapon weapon ? weapon.spec() : null;
    }
}
