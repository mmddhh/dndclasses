package mdh.dndclasses.events;


import net.minecraftforge.eventbus.api.Event;

public class LevelUp extends Event {
    private final int newlevel;

    public LevelUp(int level){
        this.newlevel = level;
    }

    public int getnewlevel(){
        return newlevel;
    }
}
