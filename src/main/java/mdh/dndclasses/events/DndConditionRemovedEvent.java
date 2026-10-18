package mdh.dndclasses.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server when a condition ends on a player. */
public class DndConditionRemovedEvent extends Event {

    public enum Reason { EXPIRED, SAVE, CURED, DEATH, REPLACED, OTHER }

    private final ServerPlayer player;
    private final ResourceLocation condition;
    private final Reason reason;

    public DndConditionRemovedEvent(ServerPlayer player, ResourceLocation condition, Reason reason) {
        this.player = player;
        this.condition = condition;
        this.reason = reason;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getCondition() {
        return condition;
    }

    public Reason getReason() {
        return reason;
    }
}
