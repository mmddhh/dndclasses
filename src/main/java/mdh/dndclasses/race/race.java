package mdh.dndclasses.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraftforge.common.util.INBTSerializable;
import java.util.ArrayList;
import java.util.List;

public class race implements Irace, INBTSerializable<CompoundTag> {
    private String raceName = "";
    private String subraceName = "";
    private List<String> features = new ArrayList<>();

    @Override
    public String getRaceName() {
        return raceName;
    }

    @Override
    public void setRaceName(String name) {
        this.raceName = name != null ? name : "";
    }

    @Override
    public String getSubraceName() {
        return subraceName;
    }

    @Override
    public void setSubraceName(String name) {
        this.subraceName = name != null ? name : "";
    }

    @Override
    public List<String> getFeatures() {
        return features;
    }

    @Override
    public void setFeatures(List<String> features) {
        this.features = features != null ? new ArrayList<>(features) : new ArrayList<>();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("raceName", raceName);
        nbt.putString("subraceName", subraceName);
        ListTag list = new ListTag();
        for (String f : features) list.add(StringTag.valueOf(f));
        nbt.put("features", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("raceName")) {
            raceName = nbt.getString("raceName");
        }
        if (nbt.contains("subraceName")) {
            subraceName = nbt.getString("subraceName");
        }
        if (nbt.contains("features")) {
            ListTag list = nbt.getList("features", 8);
            features = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                features.add(list.getString(i));
            }
        }
    }
}
