package mdh.dndclasses.data;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.events.LevelUp;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.level.DndLeveling;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.network.ServerStateSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Set;

/**
 * Level-ups are applied on a long rest: XP is banked during play, then every completed long rest
 * checks whether enough XP accumulated and raises the level (possibly several at once), firing one
 * {@link LevelUp} event per level. Ability Score Improvements are queued for a choice page.
 */
public final class LevelUpManager {

    private static final Set<Integer> STANDARD_ASI_LEVELS = Set.of(4, 8, 12, 16, 19);

    private LevelUpManager() {
    }

    public static void onLongRest(ServerPlayer player) {
        Ilevel level = player.getCapability(ModCapabilities.LEVEL_CAPABILITY).orElse(null);
        if (level == null || !level.hasCreatedCharacter()) {
            return;
        }
        int target = DndLeveling.getLevelForExperience(level.getExp());
        int oldLevel = level.getlevel();
        if (target > oldLevel) {
            for (int value = oldLevel + 1; value <= target; value++) {
                level.setlevel(value);
                MinecraftForge.EVENT_BUS.post(new LevelUp(player, value - 1, value));
            }
            for (int value = oldLevel + 1; value <= target; value++) {
                if (isAsiLevel(level.getclassName(), value) && !level.getAsiLevels().contains(value)) {
                    level.getAsiLevels().add(value);
                    level.setPendingAsi(level.getPendingAsi() + 1);
                }
            }
            player.sendSystemMessage(Component.literal("长休完成：等级提升至 " + level.getlevel()
                    + "（熟练 +" + level.getproficiencybonus() + "）"));
            CharacterRefresh.refresh(player);
            ServerStateSync.send(player);
        }
        ChoiceManager.openPending(player);
    }

    public static boolean isAsiLevel(String classKey, int level) {
        if (STANDARD_ASI_LEVELS.contains(level)) {
            return true;
        }
        return switch (classKey == null ? "" : classKey) {
            case "fighter" -> level == 6 || level == 14;
            case "rogue" -> level == 10;
            default -> false;
        };
    }

    /** ASI levels still reachable by this class (for the choice page). */
    public static List<Integer> asiLevelsFor(String classKey) {
        return switch (classKey == null ? "" : classKey) {
            case "fighter" -> List.of(4, 6, 8, 12, 14, 16, 19);
            case "rogue" -> List.of(4, 8, 10, 12, 16, 19);
            default -> List.of(4, 8, 12, 16, 19);
        };
    }
}
