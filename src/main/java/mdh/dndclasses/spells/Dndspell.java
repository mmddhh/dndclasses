package mdh.dndclasses.spells;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Implementation of the per-player spell state. Lists are ordered (LinkedHashSet) so the client
 * UI is deterministic.
 */
public class Dndspell implements IDndSpell {

    private final HashMap<Integer, Integer> spellslots = new HashMap<>();
    private final HashMap<Integer, Integer> maxspellslots = new HashMap<>();

    private Preparation preparation = Preparation.NONE;
    private int knownCantripLimit = 0;
    private int knownSpellLimit = 0;
    private int preparedSpellLimit = 0;

    private final LinkedHashSet<ResourceLocation> knownCantrips = new LinkedHashSet<>();
    private final LinkedHashSet<ResourceLocation> knownSpells = new LinkedHashSet<>();
    private final LinkedHashSet<ResourceLocation> preparedSpells = new LinkedHashSet<>();

    public Dndspell() {
        for (int level = 1; level <= 9; level++) {
            spellslots.put(level, 0);
            maxspellslots.put(level, 0);
        }
    }

    // ------------------------------------------------------------- slots

    @Override
    public int getmaxspellslots(int slotslevel) {
        return maxspellslots.getOrDefault(slotslevel, 0);
    }

    @Override
    public int getspellslots(int slotslevel) {
        return spellslots.getOrDefault(slotslevel, 0);
    }

    @Override
    public void setspellslots(int slotslevel, int slots) {
        if (!isValidSlotLevel(slotslevel)) {
            return;
        }
        spellslots.put(slotslevel, Math.min(Math.max(0, slots), getmaxspellslots(slotslevel)));
    }

    @Override
    public void setmaxspellslots(int slotslevel, int maxslots) {
        if (!isValidSlotLevel(slotslevel)) {
            return;
        }
        maxspellslots.put(slotslevel, Math.max(0, maxslots));
        setspellslots(slotslevel, getspellslots(slotslevel));
    }

    @Override
    public void costslots(int slotslevel, int slots) {
        if (!isValidSlotLevel(slotslevel) || slots <= 0) {
            return;
        }
        spellslots.put(slotslevel, Math.max(0, getspellslots(slotslevel) - slots));
    }

    @Override
    public void reslots() {
        for (int level = 1; level <= 9; level++) {
            spellslots.put(level, getmaxspellslots(level));
        }
    }

    @Override
    public void spreslots(int slotlevel, int slots) {
        if (!isValidSlotLevel(slotlevel) || slots <= 0) {
            return;
        }
        setspellslots(slotlevel, getspellslots(slotlevel) + slots);
    }

    // --------------------------------------------------- preparation/limits

    @Override
    public Preparation getPreparation() {
        return preparation;
    }

    @Override
    public void setPreparation(Preparation preparation) {
        this.preparation = preparation == null ? Preparation.NONE : preparation;
    }

    @Override
    public int getKnownCantripLimit() {
        return knownCantripLimit;
    }

    @Override
    public int getKnownSpellLimit() {
        return knownSpellLimit;
    }

    @Override
    public int getPreparedSpellLimit() {
        return preparedSpellLimit;
    }

    @Override
    public void setKnownCantripLimit(int n) {
        this.knownCantripLimit = Math.max(0, n);
    }

    @Override
    public void setKnownSpellLimit(int n) {
        this.knownSpellLimit = Math.max(0, n);
    }

    @Override
    public void setPreparedSpellLimit(int n) {
        this.preparedSpellLimit = Math.max(0, n);
    }

    // ---------------------------------------------------------------- lists

    @Override
    public Set<ResourceLocation> getKnownCantrips() {
        return Collections.unmodifiableSet(knownCantrips);
    }

    @Override
    public Set<ResourceLocation> getKnownSpells() {
        return Collections.unmodifiableSet(knownSpells);
    }

    @Override
    public Set<ResourceLocation> getPreparedSpells() {
        return Collections.unmodifiableSet(preparedSpells);
    }

    @Override
    public void setKnownCantrips(Collection<ResourceLocation> ids) {
        replace(knownCantrips, ids);
    }

    @Override
    public void setKnownSpells(Collection<ResourceLocation> ids) {
        replace(knownSpells, ids);
    }

    @Override
    public void setPreparedSpells(Collection<ResourceLocation> ids) {
        replace(preparedSpells, ids);
    }

    @Override
    public boolean addKnownCantrip(ResourceLocation id) {
        if (id == null || knownCantrips.contains(id) || knownCantrips.size() >= knownCantripLimit) {
            return false;
        }
        return knownCantrips.add(id);
    }

    @Override
    public boolean addKnownSpell(ResourceLocation id) {
        if (id == null || knownSpells.contains(id) || knownSpells.size() >= knownSpellLimit) {
            return false;
        }
        return knownSpells.add(id);
    }

    @Override
    public boolean prepareSpell(ResourceLocation id) {
        if (id == null || preparedSpells.contains(id) || preparedSpells.size() >= preparedSpellLimit) {
            return false;
        }
        return preparedSpells.add(id);
    }

    @Override
    public boolean removeKnownCantrip(ResourceLocation id) {
        return id != null && knownCantrips.remove(id);
    }

    @Override
    public boolean removeKnownSpell(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        boolean removed = knownSpells.remove(id);
        preparedSpells.remove(id);
        return removed;
    }

    @Override
    public boolean unprepareSpell(ResourceLocation id) {
        return id != null && preparedSpells.remove(id);
    }

    // -------------------------------------------------------------- queries

    @Override
    public boolean knows(ResourceLocation id) {
        return id != null
                && (knownCantrips.contains(id) || knownSpells.contains(id) || preparedSpells.contains(id));
    }

    @Override
    public boolean canCast(ResourceLocation id) {
        return id != null && getCastableSpells().contains(id);
    }

    @Override
    public Set<ResourceLocation> getCastableSpells() {
        Set<ResourceLocation> result = new LinkedHashSet<>(knownCantrips);
        if (preparation == Preparation.PREPARED) {
            result.addAll(preparedSpells);
        } else if (preparation == Preparation.KNOWN) {
            result.addAll(knownSpells);
        }
        return result;
    }

    @Override
    public void enforceLimits() {
        trimToLimit(knownCantrips, knownCantripLimit);
        trimToLimit(knownSpells, knownSpellLimit);
        trimToLimit(preparedSpells, preparedSpellLimit);
    }

    // ---------------------------------------------------------------- nbt

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (int level = 1; level <= 9; level++) {
            nbt.putInt(Integer.toString(level), getspellslots(level));
            nbt.putInt(level + "max", getmaxspellslots(level));
        }
        nbt.putInt("knownCantripLimit", knownCantripLimit);
        nbt.putInt("knownSpellLimit", knownSpellLimit);
        nbt.putInt("preparedSpellLimit", preparedSpellLimit);
        nbt.putString("preparation", preparation.name().toLowerCase(Locale.ROOT));
        nbt.put("knownCantrips", writeIds(knownCantrips));
        nbt.put("knownSpells", writeIds(knownSpells));
        nbt.put("preparedSpells", writeIds(preparedSpells));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (int level = 1; level <= 9; level++) {
            String key = Integer.toString(level);
            String maxKey = key + "max";
            if (nbt.contains(maxKey)) {
                setmaxspellslots(level, nbt.getInt(maxKey));
            }
            if (nbt.contains(key)) {
                setspellslots(level, nbt.getInt(key));
            }
        }

        // new keys, with backwards-compatible fallbacks to the old count keys
        if (nbt.contains("knownCantripLimit")) {
            setKnownCantripLimit(nbt.getInt("knownCantripLimit"));
        } else if (nbt.contains("knowcantrips")) {
            setKnownCantripLimit(nbt.getInt("knowcantrips"));
        }
        if (nbt.contains("knownSpellLimit")) {
            setKnownSpellLimit(nbt.getInt("knownSpellLimit"));
        } else if (nbt.contains("knowspell")) {
            setKnownSpellLimit(nbt.getInt("knowspell"));
        }
        setPreparedSpellLimit(nbt.contains("preparedSpellLimit")
                ? nbt.getInt("preparedSpellLimit")
                : knownSpellLimit);

        if (nbt.contains("preparation")) {
            try {
                preparation = Preparation.valueOf(nbt.getString("preparation").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                preparation = Preparation.NONE;
            }
        } else {
            preparation = Preparation.NONE;
        }

        readIds(nbt, "knownCantrips", knownCantrips);
        readIds(nbt, "knownSpells", knownSpells);
        readIds(nbt, "preparedSpells", preparedSpells);
    }

    // -------------------------------------------------------------- helpers

    private boolean isValidSlotLevel(int slotslevel) {
        return slotslevel >= 1 && slotslevel <= 9;
    }

    private static void replace(LinkedHashSet<ResourceLocation> target, Collection<ResourceLocation> ids) {
        target.clear();
        if (ids != null) {
            for (ResourceLocation id : ids) {
                if (id != null) {
                    target.add(id);
                }
            }
        }
    }

    private static <T> void trimToLimit(LinkedHashSet<T> set, int limit) {
        int effective = Math.max(0, limit);
        if (set.size() <= effective) {
            return;
        }
        Iterator<T> iterator = set.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            iterator.next();
            if (index >= effective) {
                iterator.remove();
            }
            index++;
        }
    }

    private static ListTag writeIds(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static void readIds(CompoundTag nbt, String key, LinkedHashSet<ResourceLocation> target) {
        target.clear();
        if (!nbt.contains(key)) {
            return;
        }
        ListTag list = nbt.getList(key, 8); // 8 = StringTag
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                target.add(id);
            }
        }
    }
}
