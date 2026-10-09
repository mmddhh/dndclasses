package mdh.dndclasses.spells;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collection;
import java.util.Set;

/**
 * Per-player spell state. dndclasses only stores spell ids + limits + preparation mode; it does
 * not know the spell catalog (definitions, class lists, legality) - that is dndspell's job.
 */
public interface IDndSpell extends INBTSerializable<CompoundTag> {

    // ---- spell slots ----
    int getmaxspellslots(int slotslevel);

    int getspellslots(int slotslevel);

    void setspellslots(int slotslevel, int slots);

    void setmaxspellslots(int slotlevel, int maxslots);

    void costslots(int slotslevel, int slots);

    void reslots();

    void spreslots(int slotlevel, int slots);

    // ---- preparation mode ----
    Preparation getPreparation();

    void setPreparation(Preparation preparation);

    // ---- limits (counts, not lists) ----
    int getKnownCantripLimit();

    int getKnownSpellLimit();

    int getPreparedSpellLimit();

    void setKnownCantripLimit(int n);

    void setKnownSpellLimit(int n);

    void setPreparedSpellLimit(int n);

    // ---- lists ----
    Set<ResourceLocation> getKnownCantrips();

    Set<ResourceLocation> getKnownSpells();

    Set<ResourceLocation> getPreparedSpells();

    void setKnownCantrips(Collection<ResourceLocation> ids);

    void setKnownSpells(Collection<ResourceLocation> ids);

    void setPreparedSpells(Collection<ResourceLocation> ids);

    /** @return false when null, duplicate, or over the cantrip limit. */
    boolean addKnownCantrip(ResourceLocation id);

    /** @return false when null, duplicate, or over the known-spell limit. */
    boolean addKnownSpell(ResourceLocation id);

    /** @return false when null, duplicate, or over the prepared-spell limit. */
    boolean prepareSpell(ResourceLocation id);

    boolean removeKnownCantrip(ResourceLocation id);

    /** Also removes from preparedSpells (keeps a wizard spellbook) as a convenience. */
    boolean removeKnownSpell(ResourceLocation id);

    boolean unprepareSpell(ResourceLocation id);

    // ---- queries ----
    /** True if the id appears in any of the three lists. */
    boolean knows(ResourceLocation id);

    /** True if the id is currently castable (see {@link #getCastableSpells()}). */
    boolean canCast(ResourceLocation id);

    /**
     * Cantrips always; leveled spells depend on the preparation mode
     * (PREPARED -> preparedSpells, KNOWN -> knownSpells, NONE -> none).
     */
    Set<ResourceLocation> getCastableSpells();

    /** Trims the three lists down to their limits (called after limits change). */
    void enforceLimits();
}
