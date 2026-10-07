package mdh.dndclasses.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import java.util.List;

public interface Irace extends INBTSerializable<CompoundTag> {
    String getRaceName();
    void setRaceName(String name);
    String getSubraceName();
    void setSubraceName(String name);
    List<String> getFeatures();
    void setFeatures(List<String> features);
}
