package mdh.dndclasses.spskill;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class skilldata implements INBTSerializable<CompoundTag> {
    private String skillname;
    private int maxskilluse;
    private int skilluse;
    private boolean rest;
    //在此处，布尔值rest为true时，此技能可以短休恢复，反之则只能长休恢复
    public skilldata(String skillname,int maxuse,boolean rest){
        this.skillname = skillname;
        this.maxskilluse = Math.max(0, maxuse);
        this.skilluse= this.maxskilluse;
        this.rest = rest;
    }
    public skilldata (){

    }

    public String getSkillname(){
        return skillname;
    }

    public int getSkilluse(){
        return skilluse;
    }

    public int getMaxskilluse(){
        return maxskilluse;
    }

    public void setMaxskilluse(int use){
        this.maxskilluse = Math.max(0, use);
        if (skilluse > maxskilluse) {
            skilluse = maxskilluse;
        }
    }

    public void setSkilluse(int use){
        this.skilluse = Math.min(Math.max(0, use), maxskilluse);
    }

    public void costskilluse(){
        if (skilluse>0){
            this.skilluse = skilluse-1;
        }
    }

    public void reskilluse(){
        this.skilluse = maxskilluse;
    }
    //个人能力问题，这个方法实际上是用来取反的，用于修改是否可以短休恢复
    public void setRest(){
        this.rest = !rest;
    }

    public boolean getrest(){
        return rest;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("skillname", skillname);
        tag.putInt("maxskilluse", maxskilluse);
        tag.putInt("skilluse", skilluse);
        tag.putBoolean("rest", rest);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("skillname")) {
            this.skillname = nbt.getString("skillname");
        }
        if (nbt.contains("maxskilluse")) {
            setMaxskilluse(nbt.getInt("maxskilluse"));
        }
        if (nbt.contains("skilluse")) {
            setSkilluse(nbt.getInt("skilluse"));
        }
        if (nbt.contains("rest")) {
            this.rest = nbt.getBoolean("rest");
        }
    }
}
