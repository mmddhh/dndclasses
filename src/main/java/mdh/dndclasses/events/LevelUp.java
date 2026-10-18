package mdh.dndclasses.events;


import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server whenever a player's D&D level increases (XP gains, commands). */
public class LevelUp extends Event {
    private final ServerPlayer player;
    private final int oldLevel;
    private final int newLevel;

    public LevelUp(ServerPlayer player, int oldLevel, int newLevel){
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public ServerPlayer getPlayer(){
        return player;
    }

    public int getOldLevel(){
        return oldLevel;
    }

    public int getNewLevel(){
        return newLevel;
    }
}
