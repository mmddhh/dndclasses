package mdh.dndclasses.spells;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;

public class Dndspell implements IDndSpell, INBTSerializable<CompoundTag> {
    HashMap<Integer,Integer>spellslots = new HashMap<>();
    HashMap<Integer,Integer>maxspellslots = new HashMap<>();

    int knowspell = 2;
    int knowcantrips = 2;


    public Dndspell(){
        spellslots.put(1,2);
        spellslots.put(2,0);
        spellslots.put(3,0);
        spellslots.put(4,0);
        spellslots.put(5,0);
        spellslots.put(6,0);
        spellslots.put(7,0);
        spellslots.put(8,0);
        spellslots.put(9,0);
        maxspellslots.put(1,2);
        maxspellslots.put(2,0);
        maxspellslots.put(3,0);
        maxspellslots.put(4,0);
        maxspellslots.put(5,0);
        maxspellslots.put(6,0);
        maxspellslots.put(7,0);
        maxspellslots.put(8,0);
        maxspellslots.put(9,0);
    }


    @Override
    public int getmaxspellslots(int slotslevel) {
        return maxspellslots.getOrDefault(slotslevel, 0);
    }

    @Override
    public int getspellslots(int slotslevel) {
        return spellslots.getOrDefault(slotslevel, 0);
    }

    @Override
    public void setspellslots(int slotslevel, int slots) {
        if (!isValidSlotLevel(slotslevel)) {
            return;
        }

        spellslots.put(slotslevel, Math.min(Math.max(0, slots), getmaxspellslots(slotslevel)));
    }

    @Override
    public  void  setmaxspellslots(int slotslevel,int maxslots){
        if (!isValidSlotLevel(slotslevel)) {
            return;
        }

        int normalizedMaxSlots = Math.max(0, maxslots);
        maxspellslots.put(slotslevel, normalizedMaxSlots);
        setspellslots(slotslevel, getspellslots(slotslevel));

    }
    @Override
    public void costslots(int slotslevel,int slots){
        if (!isValidSlotLevel(slotslevel) || slots <= 0) {
            return;
        }

        spellslots.put(slotslevel, Math.max(0, getspellslots(slotslevel) - slots));
    }

    @Override
    public void reslots() {
        int[] a={1,2,3,4,5,6,7,8,9};
        for (int b:a){
            spellslots.put(b, getmaxspellslots(b));
        }
    }

    @Override
    public void spreslots(int slotlevel, int slots) {
        if (!isValidSlotLevel(slotlevel) || slots <= 0) {
            return;
        }

        setspellslots(slotlevel, getspellslots(slotlevel) + slots);
    }

    @Override
    public int getknowspells() {
        return knowspell;
    }

    @Override
    public void setknowspells(int knowspell) {
        this.knowspell = Math.max(0, knowspell);
    }

    @Override
    public int getknowcantrips() {
        return knowcantrips;
    }

    @Override
    public void setknowcantrips(int knowcantrips) {
        this.knowcantrips = Math.max(0, knowcantrips);
    }

    private boolean isValidSlotLevel(int slotslevel) {
        return slotslevel >= 1 && slotslevel <= 9;
    }


    @Override
    public CompoundTag serializeNBT(){
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("1",getspellslots(1));
        nbt.putInt("2",getspellslots(2));
        nbt.putInt("3",getspellslots(3));
        nbt.putInt("4",getspellslots(4));
        nbt.putInt("5",getspellslots(5));
        nbt.putInt("6",getspellslots(6));
        nbt.putInt("7",getspellslots(7));
        nbt.putInt("8",getspellslots(8));
        nbt.putInt("9",getspellslots(9));
        nbt.putInt("1max",getmaxspellslots(1));
        nbt.putInt("2max",getmaxspellslots(2));
        nbt.putInt("3max",getmaxspellslots(3));
        nbt.putInt("4max",getmaxspellslots(4));
        nbt.putInt("5max",getmaxspellslots(5));
        nbt.putInt("6max",getmaxspellslots(6));
        nbt.putInt("7max",getmaxspellslots(7));
        nbt.putInt("8max",getmaxspellslots(8));
        nbt.putInt("9max",getmaxspellslots(9));
        nbt.putInt("knowspell",getknowspells());
        nbt.putInt("knowcantrips",getknowcantrips());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (int slotLevel = 1; slotLevel <= 9; slotLevel++) {
            String key = Integer.toString(slotLevel);
            String maxKey = key + "max";

            if (nbt.contains(maxKey)) {
                setmaxspellslots(slotLevel, nbt.getInt(maxKey));
            }
            if (nbt.contains(key)) {
                setspellslots(slotLevel, nbt.getInt(key));
            }
        }

        if (nbt.contains("knowspell")) {
            setknowspells(nbt.getInt("knowspell"));
        }
        if (nbt.contains("knowcantrips")) {
            setknowcantrips(nbt.getInt("knowcantrips"));
        }
    }
}
