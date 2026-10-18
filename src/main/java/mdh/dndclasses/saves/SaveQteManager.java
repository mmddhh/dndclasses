package mdh.dndclasses.saves;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.events.DndConcentrationBrokenEvent;
import mdh.dndclasses.events.DndSaveResolvedEvent;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.network.OpenSaveQtePacket;
import mdh.dndclasses.network.ServerStateSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side save / check resolution.
 * <ul>
 *   <li>{@link Mode#HIDDEN}: rolled immediately with a real d20 (like tabletop).</li>
 *   <li>{@link Mode#TIMING}: a "judgement box + sliding blocks" QTE (box width = total save bonus,
 *       block speed = difficulty from the DC). Several requests coalesce into additional blocks.</li>
 *   <li>{@link Mode#MASH}: a rapid-click QTE (per-click value scales with the save bonus, difficulty
 *       raises both the required total and a per-second decay).</li>
 * </ul>
 * Each slot is resolved independently: the client reports a per-slot result the moment it is judged
 * (a press that lands outside the box fails that block immediately), the server fires
 * {@link DndSaveResolvedEvent} for it right away, and breaks concentration when a CON slot fails.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveQteManager {

    public enum Mode { TIMING, MASH, HIDDEN }

    private static final int GRACE_TICKS = 12;

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static long nextId = 1L;

    private SaveQteManager() {
    }

    // ------------------------------------------------------------ request

    public static void request(ServerPlayer player, String ability, int dc, Mode mode) {
        if (player == null) {
            return;
        }
        if (mode == Mode.HIDDEN) {
            int roll = 1 + player.getRandom().nextInt(20);
            boolean success = roll + Saves.getSaveBonus(player, ability) >= dc;
            MinecraftForge.EVENT_BUS.post(new DndSaveResolvedEvent(player, ability, dc, "hidden", roll, success));
            sendResultMessage(player, ability, success);
            return;
        }

        UUID uuid = player.getUUID();
        Active active = ACTIVE.get(uuid);
        if (mode == Mode.MASH) {
            if (active != null) {
                finalize(active);
            }
            float difficulty = difficulty(dc);
            active = new Active(Long.toString(nextId++), uuid, Mode.MASH, serverTick(player));
            active.perClick = (int) clamp(6 + Saves.getSaveBonus(player, ability), 1, 12);
            active.required = (int) Math.round(18 + 60 * difficulty);
            active.decayPerSecond = (float) (3 + 12 * difficulty);
            active.timeLimitSeconds = (float) clamp(3.0 - 1.8 * difficulty, 1.0, 3.0);
            active.addSlot(ability, dc, Saves.getSaveBonus(player, ability));
            active.deadlineTick = active.startTick + Math.round(active.timeLimitSeconds * 20f) + GRACE_TICKS;
            ACTIVE.put(uuid, active);
            sendOpen(player, active);
            return;
        }

        // TIMING
        if (active != null && active.mode != Mode.TIMING) {
            finalize(active);
            active = null;
        }
        if (active == null) {
            active = new Active(Long.toString(nextId++), uuid, Mode.TIMING, serverTick(player));
            ACTIVE.put(uuid, active);
        }
        Slot slot = active.addSlot(ability, dc, Saves.getSaveBonus(player, ability));
        double periodSeconds = 2.8 - 2.4 * difficulty(dc);
        slot.periodSeconds = (float) periodSeconds;
        long tripTicks = Math.round(2.0 * periodSeconds * 20.0);
        active.deadlineTick = Math.max(active.deadlineTick, slot.spawnTick + tripTicks + GRACE_TICKS);
        sendOpen(player, active);
    }

    // ------------------------------------------------------------ resolve

    /** Per-slot result from the client, applied the moment the block is judged. */
    public static void resolveSlot(ServerPlayer player, String qteId, int index, boolean success) {
        Active active = ACTIVE.get(player.getUUID());
        if (active == null || !active.id.equals(qteId) || index < 0 || index >= active.slots.size()) {
            return;
        }
        Slot slot = active.slots.get(index);
        if (slot.resolved) {
            return;
        }
        slot.resolved = true;
        slot.success = success;
        MinecraftForge.EVENT_BUS.post(new DndSaveResolvedEvent(
                player, slot.ability, slot.dc, active.mode.name().toLowerCase(), -1, success));
        if ("con".equals(slot.ability) && !success) {
            breakConcentration(player, DndConcentrationBrokenEvent.Reason.DAMAGE);
        }
        if (active.allResolved()) {
            finalize(active);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        long now = event.getServer().getTickCount();
        List<Active> expired = new ArrayList<>();
        for (Active active : ACTIVE.values()) {
            if (now >= active.deadlineTick) {
                expired.add(active);
            }
        }
        for (Active active : expired) {
            finalize(active);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    /** Resolves any still-unresolved slots as failures, posts their events, and sends the summary. */
    private static void finalize(Active active) {
        ACTIVE.remove(active.player);
        ServerPlayer player = active.resolvePlayer();
        if (player == null || active.slots.isEmpty()) {
            return;
        }
        boolean allSuccess = true;
        for (Slot slot : active.slots) {
            if (!slot.resolved) {
                slot.resolved = true;
                slot.success = false;
                MinecraftForge.EVENT_BUS.post(new DndSaveResolvedEvent(
                        player, slot.ability, slot.dc, active.mode.name().toLowerCase(), -1, false));
                if ("con".equals(slot.ability)) {
                    breakConcentration(player, DndConcentrationBrokenEvent.Reason.DAMAGE);
                }
            }
            if (!slot.success) {
                allSuccess = false;
            }
        }
        sendResultMessage(player, active.slots.get(0).ability, allSuccess);
    }

    /** Breaks the player's concentration (if any) and fires the event. */
    public static void breakConcentration(ServerPlayer player, DndConcentrationBrokenEvent.Reason reason) {
        boolean[] broken = {false};
        ResourceLocation[] id = {null};
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
            ResourceLocation current = spell.getConcentration();
            if (current != null) {
                spell.clearConcentration();
                id[0] = current;
                broken[0] = true;
            }
        });
        if (broken[0]) {
            MinecraftForge.EVENT_BUS.post(new DndConcentrationBrokenEvent(player, id[0], reason));
            ServerStateSync.send(player);
        }
    }

    // ------------------------------------------------------------ helpers

    private static void sendOpen(ServerPlayer player, Active active) {
        JsonObject root = new JsonObject();
        root.addProperty("id", active.id);
        root.addProperty("mode", active.mode == Mode.MASH ? "mash" : "timing");
        root.addProperty("timeoutTicks", active.deadlineTick - active.startTick);

        JsonArray blocks = new JsonArray();
        for (Slot slot : active.slots) {
            JsonObject block = new JsonObject();
            block.addProperty("ability", slot.ability);
            block.addProperty("dc", slot.dc);
            block.addProperty("boxWidth", (float) clamp(0.15 + 0.04 * slot.bonus, 0.10, 0.60));
            block.addProperty("period", slot.periodSeconds);
            blocks.add(block);
        }
        root.add("blocks", blocks);

        JsonObject mash = new JsonObject();
        mash.addProperty("perClick", active.perClick);
        mash.addProperty("required", active.required);
        mash.addProperty("decay", active.decayPerSecond);
        mash.addProperty("timeLimit", active.timeLimitSeconds);
        root.add("mash", mash);

        DndNetwork.sendToPlayer(player, new OpenSaveQtePacket(active.id, root.toString()));
    }

    private static void sendResultMessage(ServerPlayer player, String ability, boolean success) {
        Component abilityName = Component.translatable("ability.dndclasses." + ability);
        Component message = (success
                ? Component.translatable("message.dndclasses.save.success", abilityName)
                : Component.translatable("message.dndclasses.save.fail", abilityName))
                .withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED);
        player.displayClientMessage(message, true);
    }

    private static float difficulty(int dc) {
        return (float) clamp((dc - 10) / 8.0, 0.0, 1.0);
    }

    private static long serverTick(ServerPlayer player) {
        net.minecraft.server.MinecraftServer server = player.getServer();
        return server == null ? 0L : server.getTickCount();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------ model

    private static final class Slot {
        final String ability;
        final int dc;
        final int bonus;
        final long spawnTick;
        float periodSeconds;
        boolean resolved;
        boolean success;

        Slot(String ability, int dc, int bonus, long spawnTick) {
            this.ability = ability;
            this.dc = dc;
            this.bonus = bonus;
            this.spawnTick = spawnTick;
        }
    }

    private static final class Active {
        final String id;
        final UUID player;
        final Mode mode;
        final long startTick;
        final List<Slot> slots = new ArrayList<>();
        long deadlineTick;
        int perClick;
        int required;
        float decayPerSecond;
        float timeLimitSeconds;

        Active(String id, UUID player, Mode mode, long startTick) {
            this.id = id;
            this.player = player;
            this.mode = mode;
            this.startTick = startTick;
            this.deadlineTick = startTick;
        }

        Slot addSlot(String ability, int dc, int bonus) {
            Slot slot = new Slot(ability, dc, bonus, startTick);
            slots.add(slot);
            return slot;
        }

        boolean allResolved() {
            for (Slot slot : slots) {
                if (!slot.resolved) {
                    return false;
                }
            }
            return true;
        }

        ServerPlayer resolvePlayer() {
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            return server == null ? null : server.getPlayerList().getPlayer(player);
        }
    }
}
