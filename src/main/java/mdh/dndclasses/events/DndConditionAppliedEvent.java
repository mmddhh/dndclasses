package mdh.dndclasses.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/** Fired on the server when a condition is applied (or refreshed) on a player. */
public class DndConditionAppliedEvent extends Event {

    private final ServerPlayer player;
    private final ResourceLocation condition;
    private final int durationTicks;
    private final UUID source;

    public DndConditionAppliedEvent(ServerPlayer player, ResourceLocation condition, int durationTicks, UUID source) {
        this.player = player;
        this.condition = condition;
        this.durationTicks = durationTicks;
        this.source = source;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getCondition() {
        return condition;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public UUID getSource() {
        return source;
    }
}
