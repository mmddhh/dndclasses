package mdh.dndclasses.spells;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IDndSpell extends INBTSerializable<CompoundTag> {
    int getmaxspellslots(int slotslevel);
    int getspellslots(int slotslevel);
    void setspellslots(int slotslevel,int slots);
    void setmaxspellslots(int slotlevel,int maxslots);
    void costslots(int slotslevel,int slots);
    void reslots();
    void spreslots(int slotlevel,int slots);
    int getknowspells();
    void setknowspells(int knowspell);
    int getknowcantrips();
    void setknowcantrips(int knowcantrips);
}
