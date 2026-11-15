package mdh.dndclasses.worldgen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;

/**
 * First entity adapter for the plan. Reuses the vanilla villager with persistent plan tags; a
 * custom NPC entity can replace this later without changing the {@link NpcProfile} hand-off.
 */
public final class NpcSpawner {

    public static final String TAG_NATION = "DndNation";
    public static final String TAG_SETTLEMENT = "DndSettlement";
    public static final String TAG_CULTURE = "DndCulture";
    public static final String TAG_ROLE = "DndRole";

    private NpcSpawner() {
    }

    public static Villager spawn(ServerPlayer player, NpcProfile profile) {
        ServerLevel level = player.serverLevel();
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) {
            return null;
        }
        villager.moveTo(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D,
                player.getYRot(), 0.0F);
        villager.setCustomName(Component.literal(profile.name()).withStyle(ChatFormatting.GOLD));
        villager.setCustomNameVisible(true);
        villager.setVillagerData(villager.getVillagerData()
                .setProfession(professionFor(profile.role()))
                .setLevel(2)
                .setType(VillagerType.PLAINS));
        villager.getPersistentData().putString(TAG_NATION, Integer.toString(profile.nationId()));
        villager.getPersistentData().putString(TAG_SETTLEMENT, Integer.toString(profile.settlementId()));
        villager.getPersistentData().putString(TAG_CULTURE, profile.culture());
        villager.getPersistentData().putString(TAG_ROLE, profile.role());
        villager.setPersistenceRequired();
        level.addFreshEntity(villager);
        return villager;
    }

    private static VillagerProfession professionFor(String role) {
        return switch (role) {
            case "merchant" -> VillagerProfession.CARTOGRAPHER;
            case "guard" -> VillagerProfession.WEAPONSMITH;
            case "scholar" -> VillagerProfession.LIBRARIAN;
            case "priest" -> VillagerProfession.CLERIC;
            default -> VillagerProfession.FARMER;
        };
    }
}
