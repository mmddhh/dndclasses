package mdh.dndclasses.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface Iability extends INBTSerializable<CompoundTag> {
    int getstr();
    void setstr(int str);
    int getdex();
    void setdex(int dex);
    int getcon();
    void setcon(int con);
    int getint();
    void setint(int INT);
    int getwis();
    void setwis(int wis);
    int getcha();
    void setcha(int cha);
    int getstrmod();
    int getdexmod();
    int getconmod();
    int getintmod();
    int getwismod();
    int getchamod();

}
