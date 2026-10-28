package mdh.dndclasses.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.List;

/** Capability attached to every {@code LivingEntity}: projectiles stuck in that entity. */
public interface IStuckProjectiles extends INBTSerializable<CompoundTag> {

    /** Read-only live view; do not mutate through this list. */
    List<StuckProjectile> getAll();

    /** Replaces the whole list (client sync, NBT load). */
    void setAll(List<StuckProjectile> projectiles);

    /** Appends one projectile, evicting the oldest entry when the cap is reached. */
    void add(StuckProjectile projectile);

    boolean remove(int id);

    void clear();

    int size();
}
