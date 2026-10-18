package mdh.dndclasses.network;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.conditions.ConditionManager;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.feature.FeatureManager;
import mdh.dndclasses.level.DndLeveling;
import mdh.dndclasses.network.ClientboundCharacterStatePacket.FeatureEntry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds and pushes the character-state snapshot (capability NBT + active features) to a player's client. */
public final class ServerStateSync {

    private ServerStateSync() {
    }

    public static void send(ServerPlayer player) {
        CharacterRefresh.refresh(player);  // refresh limits/slots/preparation BEFORE snapshotting
        send(player, true, false);
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
                            List<FeatureEntry> features = created ? buildFeatures(player, currentLevel) : List.of();

                            DndNetwork.sendToPlayer(player, new ClientboundCharacterStatePacket(
                                    created, promptOpen,
                                    level.serializeNBT(), ability.serializeNBT(), spell.serializeNBT(),
                                    ConditionManager.buildClientTag(player),
                                    levelStart, expToNext, features));
                        })));
    }

    private static List<FeatureEntry> buildFeatures(ServerPlayer player, int playerLevel) {
        List<FeatureEntry> entries = new ArrayList<>();
        for (FeatureDefinition feature : FeatureManager.activeFeatureList(player)) {
            if (feature.kind() != FeatureDefinition.Kind.ACTIVE) {
                continue;
            }
            String nameKey = feature.nameKey();
            if (nameKey == null || nameKey.isEmpty()) {
                nameKey = "feature.dndclasses." + feature.id().getPath();
            }
            entries.add(new FeatureEntry(
                    feature.id(),
                    nameKey,
                    FeatureManager.remainingUses(player, feature.id()),
                    feature.usesAtLevel(playerLevel),
                    feature.rest().name().toLowerCase(Locale.ROOT)));
        }
        return entries;
    }
}
