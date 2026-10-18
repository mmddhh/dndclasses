package mdh.dndclasses.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the server when a player's class changes (respec / rebuild). No trigger point exists
 * yet; the event is provided so downstream mods can subscribe once a respec path is added.
 */
public class DndClassChangedEvent extends Event {

    private final ServerPlayer player;
    private final String oldKey;
    private final String newKey;

    public DndClassChangedEvent(ServerPlayer player, String oldKey, String newKey) {
        this.player = player;
        this.oldKey = oldKey == null ? "" : oldKey;
        this.newKey = newKey == null ? "" : newKey;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getOldKey() {
        return oldKey;
    }

    public String getNewKey() {
        return newKey;
    }
}
