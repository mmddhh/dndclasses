package mdh.dndclasses.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server when a player completes a short rest. */
public class DndShortRestEvent extends Event {

    private final ServerPlayer player;

    public DndShortRestEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
