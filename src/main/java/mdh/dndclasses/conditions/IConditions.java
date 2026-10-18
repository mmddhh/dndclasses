package mdh.dndclasses.conditions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;

/** Per-player active D&amp;D conditions plus the exhaustion level. */
public interface IConditions extends INBTSerializable<CompoundTag> {

    Map<ResourceLocation, ActiveCondition> getActive();

    ActiveCondition get(ResourceLocation id);

    void put(ResourceLocation id, ActiveCondition condition);

    void remove(ResourceLocation id);

    /** Removes every condition but keeps exhaustion. */
    void clearNonExhaustion();

    int getExhaustionLevel();

    void setExhaustionLevel(int level);
}
