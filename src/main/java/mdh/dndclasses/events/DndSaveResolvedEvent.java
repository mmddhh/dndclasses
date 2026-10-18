package mdh.dndclasses.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the server after a saving throw / check is resolved by the save-QTE system.
 * {@code mode} is {@code "timing"}, {@code "mash"} or {@code "hidden"}; {@code roll} is the d20 for
 * hidden saves and {@code -1} for the skill-based modes.
 */
public class DndSaveResolvedEvent extends Event {

    private final ServerPlayer player;
    private final String ability;
    private final int dc;
    private final String mode;
    private final int roll;
    private final boolean success;

    public DndSaveResolvedEvent(ServerPlayer player, String ability, int dc, String mode, int roll, boolean success) {
        this.player = player;
        this.ability = ability == null ? "" : ability;
        this.dc = dc;
        this.mode = mode == null ? "" : mode;
        this.roll = roll;
        this.success = success;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getAbility() {
        return ability;
    }

    public int getDc() {
        return dc;
    }

    public String getMode() {
        return mode;
    }

    public int getRoll() {
        return roll;
    }

    public boolean isSuccess() {
        return success;
    }
}
