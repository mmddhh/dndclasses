package mdh.dndclasses.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;

import static java.lang.Math.floorDiv;

public class ability implements Iability, INBTSerializable<CompoundTag> {

    HashMap<String,Integer>Ability = new HashMap<String,Integer>();
    HashMap<String,Integer>Modifier = new HashMap<String,Integer>();

    public ability(){
            Ability.put("str",8);
            Ability.put("dex",8);
            Ability.put("con",8);
            Ability.put("int",8);
            Ability.put("wis",8);
            Ability.put("cha",8);
            Modifier.put("strmod",Math.floorDiv(8 - 10, 2));
            Modifier.put("dexmod",Math.floorDiv(8 - 10, 2));
            Modifier.put("conmod",Math.floorDiv(8 - 10, 2));
            Modifier.put("intmod",Math.floorDiv(8 - 10, 2));
            Modifier.put("wismod",Math.floorDiv(8 - 10, 2));
            Modifier.put("chamod",Math.floorDiv(8 - 10, 2));
    }




    @Override
    public int getstr() {
        return Ability.get("str");
    }

    @Override
    public void setstr(int str) {
        Ability.put("str",str);
        Modifier.put("strmod",Math.floorDiv(str - 10, 2));
    }

    @Override
    public int getstrmod(){
        return Modifier.get("strmod");
    }

    @Override
    public int getdex() {
        return Ability.get("dex");
    }

    @Override
    public void setdex(int dex) {
        Ability.put("dex",dex);
        Modifier.put("dexmod",Math.floorDiv(dex - 10, 2));
    }

    @Override
    public int getdexmod(){
        return Modifier.get("dexmod");
    }

    @Override
    public int getcon() {
        return Ability.get("con");
    }

    @Override
    public void setcon(int con) {
        Ability.put("con",con);
        Modifier.put("conmod",Math.floorDiv(con - 10, 2));
    }

    @Override
    public int getconmod(){
        return Modifier.get("conmod");
    }

    @Override
    public int getint() {
        return Ability.get("int");
    }

    @Override
    public void setint(int INT) {
        Ability.put("int",INT);
        Modifier.put("intmod",Math.floorDiv(INT - 10, 2));
    }

    @Override
    public int getintmod(){
        return Modifier.get("intmod");
    }

    @Override
    public int getwis() {
        return Ability.get("wis");
    }

    @Override
    public void setwis(int wis) {
        Ability.put("wis",wis);
        Modifier.put("wismod",Math.floorDiv(wis - 10, 2));
    }

    @Override
    public int getwismod(){
        return Modifier.get("wismod");
    }

    @Override
    public int getcha() {
        return Ability.get("cha");
    }

    @Override
    public void setcha(int cha) {
        Ability.put("cha",cha);
        Modifier.put("chamod",Math.floorDiv(cha - 10, 2));
    }

    @Override
    public int getchamod(){
        return Modifier.get("chamod");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("str",Ability.get("str"));
        nbt.putInt("dex",Ability.get("dex"));
        nbt.putInt("con",Ability.get("con"));
        nbt.putInt("int",Ability.get("int"));
        nbt.putInt("wis",Ability.get("wis"));
        nbt.putInt("cha",Ability.get("cha"));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("str")) {
            setstr(nbt.getInt("str"));
        }
        if (nbt.contains("dex")) {
            setdex(nbt.getInt("dex"));
        }
        if (nbt.contains("con")) {
            setcon(nbt.getInt("con"));
        }
        if (nbt.contains("int")) {
            setint(nbt.getInt("int"));
        }
        if (nbt.contains("wis")) {
            setwis(nbt.getInt("wis"));
        }
        if (nbt.contains("cha")) {
            setcha(nbt.getInt("cha"));
        }

    }
}
