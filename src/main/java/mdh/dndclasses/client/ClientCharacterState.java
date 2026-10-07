package mdh.dndclasses.client;

import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.network.ClientboundCharacterStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side cache of the player's class-creation state. Also hydrates the local player's
 * capabilities from the synced NBT so other client code can just read them normally.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCharacterState {

    public static boolean created = false;
    public static String classKey = "";
    public static String subclassKey = "";
    public static int level = 1;
    public static int exp = 0;
    public static int proficiency = 2;
    public static int expLevelStart = 0;
    public static int expToNext = 300;
    public static int[] abilities = new int[]{8, 8, 8, 8, 8, 8};
    public static List<String> featureIds = new ArrayList<>();

    private ClientCharacterState() {
    }

    public static void apply(ClientboundCharacterStatePacket packet) {
        created = packet.created;
        expLevelStart = packet.expLevelStart;
        expToNext = packet.expToNext;
        featureIds = packet.featureIds;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(levelCap -> {
                levelCap.deserializeNBT(packet.levelData);
                created = levelCap.hasCreatedCharacter();
                classKey = levelCap.getclassName();
                subclassKey = levelCap.getSubclassKey();
                level = levelCap.getlevel();
                exp = levelCap.getExp();
                proficiency = levelCap.getproficiencybonus();
            });

            minecraft.player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(abilityCap -> {
                abilityCap.deserializeNBT(packet.abilityData);
                abilities = new int[]{
                        abilityCap.getstr(), abilityCap.getdex(), abilityCap.getcon(),
                        abilityCap.getint(), abilityCap.getwis(), abilityCap.getcha()};
            });

            minecraft.player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY)
                    .ifPresent(spellCap -> spellCap.deserializeNBT(packet.spellData));
        }

        if (packet.promptOpen && !packet.created) {
            ClientEvents.requestOpen();
        }

        if (packet.created && minecraft.screen instanceof ClassCreationScreen) {
            minecraft.setScreen(null);
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("角色创建完成。"), false);
            }
        }
    }
}
