package mdh.dndclasses.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server after a player finishes creating their character. */
public class DndCharacterCreatedEvent extends Event {

    private final ServerPlayer player;

    public DndCharacterCreatedEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
