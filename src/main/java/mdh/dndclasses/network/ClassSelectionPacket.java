package mdh.dndclasses.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import mdh.dndclasses.ability.PointBuyHelper;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.data.ClassRegistry;
import mdh.dndclasses.data.StartingEquipment;
import mdh.dndclasses.data.SubclassRegistry;
import mdh.dndclasses.events.DndCharacterCreatedEvent;
import mdh.dndclasses.level.Ilevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

public class ClassSelectionPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String json;

    public ClassSelectionPacket(String json) {
        this.json = json == null ? "" : json;
    }

    public static void encode(ClassSelectionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.json, 32767);
    }

    public static ClassSelectionPacket decode(FriendlyByteBuf buf) {
        return new ClassSelectionPacket(buf.readUtf(32767));
    }

    public static void handle(ClassSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> apply(packet, player));
        context.setPacketHandled(true);
    }

    private static void apply(ClassSelectionPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }

        try {
            boolean alreadyCreated = player.getCapability(ModCapabilities.LEVEL_CAPABILITY)
                    .map(Ilevel::hasCreatedCharacter)
                    .orElse(false);
            if (alreadyCreated) {
                player.sendSystemMessage(Component.literal("你已经创建过角色了。"));
                return;
            }

            JsonObject root = JsonParser.parseString(packet.json).getAsJsonObject();
            String classKey = root.get("class").getAsString();
            if (!ClassRegistry.isValid(classKey)) {
                player.sendSystemMessage(Component.literal("无效的职业选择。"));
                return;
            }

            JsonObject abilities = root.getAsJsonObject("abilities");
            int str = abilities.get("str").getAsInt();
            int dex = abilities.get("dex").getAsInt();
            int con = abilities.get("con").getAsInt();
            int intel = abilities.get("int").getAsInt();
            int wis = abilities.get("wis").getAsInt();
            int cha = abilities.get("cha").getAsInt();
            for (int score : new int[]{str, dex, con, intel, wis, cha}) {
                if (score < PointBuyHelper.MIN_SCORE || score > 15) {
                    player.sendSystemMessage(Component.literal("属性值超出允许范围。"));
                    return;
                }
            }
            if (!PointBuyHelper.isValid(str, dex, con, intel, wis, cha)
                    || PointBuyHelper.totalCost(str, dex, con, intel, wis, cha) != PointBuyHelper.BUDGET) {
                player.sendSystemMessage(Component.literal("属性点分配不合法。"));
                return;
            }

            int equipmentChoice = root.has("equipmentChoice") ? root.get("equipmentChoice").getAsInt() : 0;

            String subclass = root.has("subclass") ? root.get("subclass").getAsString() : "";
            if (!subclass.isEmpty()
                    && (!SubclassRegistry.isValid(classKey, subclass) || !SubclassRegistry.isSelectableAtCreation(classKey))) {
                subclass = "";
            }
            final String chosenSubclass = subclass;

            player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
                level.setclassName(classKey);
                level.setSubclassKey(chosenSubclass);
                level.setCreatedCharacter(true);
                level.setDataVersion(1);
            });
            player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(ability -> {
                ability.setstr(str);
                ability.setdex(dex);
                ability.setcon(con);
                ability.setint(intel);
                ability.setwis(wis);
                ability.setcha(cha);
            });

            List<String> items = StartingEquipment.getItems(classKey, equipmentChoice);
            for (String itemId : items) {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                if (item == null) {
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }

            player.sendSystemMessage(Component.literal("职业已创建：" + ClassRegistry.getDisplayName(classKey)));

            ServerStateSync.send(player);
            MinecraftForge.EVENT_BUS.post(new DndCharacterCreatedEvent(player));
        } catch (Exception exception) {
            LOGGER.error("Failed to apply AUI class selection payload", exception);
        }
    }
}
