package mdh.dndclasses.spskill;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public class skill implements Iskill, INBTSerializable<CompoundTag> {
    HashMap<String,skilldata>skillmap = new HashMap<>();
    public skill(){
    }

    @Override
    public int getskilluse(String skillname) {
        skilldata data = skillmap.get(skillname);
        return data == null ? 0 : data.getSkilluse();
    }

    @Override
    public int getmaxskilluse(String skillname) {
        skilldata data = skillmap.get(skillname);
        return data == null ? 0 : data.getMaxskilluse();
    }

    @Override
    public void setskilluse(String skillname, int number) {
        skilldata data = skillmap.get(skillname);
        if (data == null) {
            return;
        }
        data.setSkilluse(number);
    }

    @Override
    public void setmaxskilluse(String skillname, int number) {
        skilldata data = skillmap.get(skillname);
        if (data == null) {
            return;
        }
        data.setMaxskilluse(number);
    }

    @Override
    public void reshortskilluse() {
        for(skilldata data : skillmap.values()){
            if (data.getrest()){
                data.reskilluse();
            }
        }
    }
    //这个是因为长休时短休也恢复，所以这里的long只是为了区分
    @Override
    public void relongskilluse() {
        for(skilldata data : skillmap.values()){
            data.reskilluse();
        }
    }

    @Override
    public void addskill(String skillName, int maxUses, boolean isShortRest) {
        if (skillName == null || skillName.isBlank()) {
            return;
        }

        skilldata newSkill = new skilldata(skillName, maxUses, isShortRest);
        skillmap.put(skillName, newSkill);
    }

    @Override
    public boolean removeSkill(String skillname) {
        return skillmap.remove(skillname) != null;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<String, skilldata> entry : skillmap.entrySet()) {
            nbt.put(entry.getKey(), entry.getValue().serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        skillmap.clear();
        for (String key : nbt.getAllKeys()) {
            skilldata data = new skilldata();
            data.deserializeNBT(nbt.getCompound(key));
            String skillName = data.getSkillname();
            skillmap.put(skillName == null || skillName.isBlank() ? key : skillName, data);
        }
    }
}
