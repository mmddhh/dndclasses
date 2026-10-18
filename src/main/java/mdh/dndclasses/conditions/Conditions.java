package mdh.dndclasses.conditions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Conditions implements IConditions {

    private final Map<ResourceLocation, ActiveCondition> active = new LinkedHashMap<>();
    private int exhaustionLevel = 0;

    @Override
    public Map<ResourceLocation, ActiveCondition> getActive() {
        return active;
    }

    @Override
    public ActiveCondition get(ResourceLocation id) {
        return active.get(id);
    }

    @Override
    public void put(ResourceLocation id, ActiveCondition condition) {
        active.put(id, condition);
    }

    @Override
    public void remove(ResourceLocation id) {
        active.remove(id);
    }

    @Override
    public void clearNonExhaustion() {
        active.clear();
    }

    @Override
    public int getExhaustionLevel() {
        return exhaustionLevel;
    }

    @Override
    public void setExhaustionLevel(int level) {
        this.exhaustionLevel = Math.max(0, Math.min(6, level));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("exhaustion", exhaustionLevel);
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, ActiveCondition> entry : active.entrySet()) {
            ActiveCondition condition = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putString("id", entry.getKey().toString());
            tag.putInt("duration", condition.durationTicks);
            tag.putInt("dc", condition.dc);
            tag.putInt("save", condition.saveTicks);
            if (condition.source != null) {
                tag.putString("source", condition.source.toString());
            }
            list.add(tag);
        }
        nbt.put("conditions", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        active.clear();
        exhaustionLevel = nbt.contains("exhaustion") ? Math.max(0, Math.min(6, nbt.getInt("exhaustion"))) : 0;
        ListTag list = nbt.getList("conditions", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null) {
                continue;
            }
            UUID source = tag.contains("source") && !tag.getString("source").isEmpty()
                    ? UUID.fromString(tag.getString("source")) : null;
            active.put(id, new ActiveCondition(tag.getInt("duration"), tag.getInt("dc"), tag.getInt("save"), source));
        }
    }
}
