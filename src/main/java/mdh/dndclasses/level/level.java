package mdh.dndclasses.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraftforge.common.util.INBTSerializable;
import java.util.ArrayList;
import java.util.List;

public class level implements Ilevel, INBTSerializable<CompoundTag> {
    int level = 1;
    int Exp = 0;
    int proficiencybonus = 2;
    boolean createdCharacter = false;
    String className = "";
    String subclassKey = "";
    int dataVersion = 1;
    List<String> classFeatures = new ArrayList<>();
    int pendingAsi = 0;
    List<Integer> asiLevels = new ArrayList<>();

    @Override
    public void setlevel(int level) {
        this.level = DndLeveling.clampLevel(level);
        this.proficiencybonus = DndLeveling.getProficiencyBonus(this.level);
    }

    @Override
    public int getlevel() {
        return level;
    }

    @Override
    public void setExp(int Exp) {
        // Experience no longer levels the character immediately: level-ups are applied on a long
        // rest (see LevelUpManager), so a player can bank XP and then rest.
        this.Exp = DndLeveling.clampExperience(Exp);
    }

    @Override
    public int getExp() {
        return Exp;
    }

    @Override
    public void setproficiencybonus(int bonus) {
        this.proficiencybonus = Math.min(6, Math.max(2, bonus));
    }

    @Override
    public int getproficiencybonus() {
        return proficiencybonus;
    }

    @Override
    public boolean hasCreatedCharacter() {
        return createdCharacter;
    }

    @Override
    public void setCreatedCharacter(boolean created) {
        this.createdCharacter = created;
    }

    @Override
    public String getclassName() {
        return className;
    }

    @Override
    public void setclassName(String name) {
        this.className = name != null ? name : "";
    }

    @Override
    public String getSubclassKey() {
        return subclassKey;
    }

    @Override
    public void setSubclassKey(String key) {
        this.subclassKey = key != null ? key : "";
    }

    @Override
    public int getDataVersion() {
        return dataVersion;
    }

    @Override
    public void setDataVersion(int version) {
        this.dataVersion = version;
    }

    @Override
    public List<String> getClassFeatures() {
        return classFeatures;
    }

    @Override
    public void setClassFeatures(List<String> features) {
        this.classFeatures = features != null ? new ArrayList<>(features) : new ArrayList<>();
    }

    @Override
    public int getPendingAsi() {
        return pendingAsi;
    }

    @Override
    public void setPendingAsi(int pending) {
        this.pendingAsi = Math.max(0, pending);
    }

    @Override
    public List<Integer> getAsiLevels() {
        return asiLevels;
    }

    @Override
    public void setAsiLevels(List<Integer> levels) {
        this.asiLevels = levels != null ? new ArrayList<>(levels) : new ArrayList<>();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("level",getlevel());
        nbt.putInt("Exp",getExp());
        nbt.putInt("proficiencybonus",getproficiencybonus());
        nbt.putBoolean("createdCharacter",createdCharacter);
        nbt.putString("className",className);
        nbt.putString("subclassKey",subclassKey);
        nbt.putInt("dataVersion",dataVersion);
        nbt.putInt("pendingAsi", pendingAsi);
        nbt.putIntArray("asiLevels", asiLevels.stream().mapToInt(Integer::intValue).toArray());
        ListTag list = new ListTag();
        for (String f : classFeatures) list.add(StringTag.valueOf(f));
        nbt.put("classFeatures", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Exp")) {
            setExp(nbt.getInt("Exp"));
        }
        if (nbt.contains("level")) {
            setlevel(nbt.getInt("level"));
        } else if (nbt.contains("Exp")) {
            setlevel(DndLeveling.getLevelForExperience(nbt.getInt("Exp")));
        }

        if (nbt.contains("proficiencybonus")) {
            setproficiencybonus(nbt.getInt("proficiencybonus"));
        }
        if (nbt.contains("pendingAsi")) {
            pendingAsi = Math.max(0, nbt.getInt("pendingAsi"));
        }
        if (nbt.contains("asiLevels")) {
            asiLevels = new ArrayList<>();
            for (int value : nbt.getIntArray("asiLevels")) {
                asiLevels.add(value);
            }
        }

        if (nbt.contains("createdCharacter")) {
            createdCharacter = nbt.getBoolean("createdCharacter");
        }
        if (nbt.contains("className")) {
            className = nbt.getString("className");
        }
        if (nbt.contains("subclassKey")) {
            subclassKey = nbt.getString("subclassKey");
        }
        if (nbt.contains("dataVersion")) {
            dataVersion = nbt.getInt("dataVersion");
        }
        if (nbt.contains("classFeatures")) {
            ListTag list = nbt.getList("classFeatures", 8);
            classFeatures = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                classFeatures.add(list.getString(i));
            }
        }
    }
}
