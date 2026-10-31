package mdh.dndclasses.data;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.network.ServerStateSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Choosing a subclass after character creation.
 * <p>
 * Only classes whose subclass is picked at level 1 can choose during creation; every other class
 * reaches its subclass level later, so the choice is offered through a clickable chat prompt (and
 * the {@code /dndsubclass} command, which must be usable without operator permissions).
 */
public final class SubclassSelection {

    public enum Result { SUCCESS, NO_CHARACTER, ALREADY_CHOSEN, TOO_EARLY, INVALID }

    private SubclassSelection() {
    }

    /** Accepts both "berserker" and "dndclasses:berserker". */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.indexOf(':') >= 0 ? trimmed : "dndclasses:" + trimmed;
    }

    public static Result pick(ServerPlayer player, String rawId) {
        String id = normalize(rawId);
        if (id == null) {
            return Result.INVALID;
        }
        Result[] result = {Result.NO_CHARACTER};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            if (!level.hasCreatedCharacter()) {
                result[0] = Result.NO_CHARACTER;
                return;
            }
            String current = level.getSubclassKey();
            if (current != null && !current.isEmpty()) {
                result[0] = Result.ALREADY_CHOSEN;
                return;
            }
            SubclassRegistry.ClassSubclasses entry = SubclassRegistry.get(level.getclassName());
            if (entry == null || !entry.isValid(id)) {
                result[0] = Result.INVALID;
                return;
            }
            if (level.getlevel() < entry.selectLevel()) {
                result[0] = Result.TOO_EARLY;
                return;
            }
            level.setSubclassKey(id);
            result[0] = Result.SUCCESS;
        });

        if (result[0] == Result.SUCCESS) {
            CharacterRefresh.refresh(player);
            ServerStateSync.send(player);
            ChoiceManager.openPending(player);
        }
        return result[0];
    }

    /** Opens the subclass selection page (falls back to chat if the page cannot be shown). */
    public static void sendPrompt(ServerPlayer player) {
        ChoiceManager.openPending(player);
    }

    /** Human-readable name for a subclass id ("dndclasses:battle_master" -> "Battle Master"). */
    public static String displayName(String id) {
        String path = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /** Subclass ids the player could pick right now (empty when they cannot choose). */
    public static List<String> available(ServerPlayer player) {
        Ilevel[] holder = {null};
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> holder[0] = level);
        Ilevel level = holder[0];
        if (level == null || !level.hasCreatedCharacter()) {
            return List.of();
        }
        String current = level.getSubclassKey();
        SubclassRegistry.ClassSubclasses entry = SubclassRegistry.get(level.getclassName());
        if (entry == null || (current != null && !current.isEmpty()) || level.getlevel() < entry.selectLevel()) {
            return List.of();
        }
        return entry.ids();
    }
}
