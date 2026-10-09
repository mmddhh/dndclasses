package mdh.dndclasses.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server after a player completes a long rest (normal sleep and wake). */
public class DndLongRestEvent extends Event {

    private final ServerPlayer player;

    public DndLongRestEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
