package mdh.dndclasses.spskill;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface Iskill extends INBTSerializable<CompoundTag> {
    int getskilluse(String skillname);
    int getmaxskilluse(String skillname);
    void setskilluse(String skillname, int number);
    void setmaxskilluse(String skillname,int number);
    void reshortskilluse();
    void relongskilluse();
    void addskill(String skillName, int maxUses, boolean isShortRest);
    //deepseek写的，因为我懒了
    boolean removeSkill(String skillname);
}
