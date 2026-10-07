package mdh.dndclasses.network;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.level.DndLeveling;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Builds and pushes the character-state snapshot (capability NBT) to a player's client. */
public final class ServerStateSync {

    private ServerStateSync() {
    }

    public static void send(ServerPlayer player) {
        send(player, true, false);
        CharacterRefresh.refresh(player);
    }

    public static void sendPrompt(ServerPlayer player) {
        send(player, false, true);
    }

    private static void send(ServerPlayer player, boolean created, boolean promptOpen) {
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level ->
                player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(ability ->
                        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
                            int currentLevel = level.getlevel();
                            int levelStart = DndLeveling.getMinimumExperienceForLevel(currentLevel);
                            int expToNext = currentLevel >= DndLeveling.MAX_LEVEL
                                    ? level.getExp()
                                    : DndLeveling.getMinimumExperienceForLevel(currentLevel + 1);
                            List<String> features = created ? level.getClassFeatures() : List.of();

                            DndNetwork.sendToPlayer(player, new ClientboundCharacterStatePacket(
                                    created, promptOpen,
                                    level.serializeNBT(), ability.serializeNBT(), spell.serializeNBT(),
                                    levelStart, expToNext, features));
                        })));
    }
}
