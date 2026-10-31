package mdh.dndclasses.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import java.util.List;

public interface Ilevel extends INBTSerializable<CompoundTag> {
    void setlevel(int level);
    int getlevel();
    void setExp(int Exp);
    int getExp();
    void setproficiencybonus(int bonus);
    int getproficiencybonus();
    boolean hasCreatedCharacter();
    void setCreatedCharacter(boolean created);
    String getclassName();
    void setclassName(String name);
    String getSubclassKey();
    void setSubclassKey(String key);
    int getDataVersion();
    void setDataVersion(int version);
    List<String> getClassFeatures();
    void setClassFeatures(List<String> features);

    /** Unspent Ability Score Improvements (one per ASI level reached after a long rest). */
    int getPendingAsi();
    void setPendingAsi(int pending);

    /** Levels whose ASI has already been granted, so a level-up never grants it twice. */
    List<Integer> getAsiLevels();
    void setAsiLevels(List<Integer> levels);
}
