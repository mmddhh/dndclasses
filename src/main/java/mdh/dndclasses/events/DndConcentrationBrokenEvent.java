package mdh.dndclasses.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server when a player's concentration ends (broken, expired, replaced or died). */
public class DndConcentrationBrokenEvent extends Event {

    public enum Reason { DAMAGE, DURATION, DEATH, REPLACED, OTHER }

    private final ServerPlayer player;
    private final ResourceLocation spell;
    private final Reason reason;

    public DndConcentrationBrokenEvent(ServerPlayer player, ResourceLocation spell, Reason reason) {
        this.player = player;
        this.spell = spell;
        this.reason = reason;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getSpell() {
        return spell;
    }

    public Reason getReason() {
        return reason;
    }
}
