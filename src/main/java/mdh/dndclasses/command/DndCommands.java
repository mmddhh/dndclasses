package mdh.dndclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.data.ClassRegistry;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.feature.FeatureManager;
import mdh.dndclasses.level.DndLeveling;
import mdh.dndclasses.network.ServerStateSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DndCommands {

    private DndCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("dnd")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource())))
                .then(Commands.literal("xp")
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> xpAdd(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "amount")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> xpSet(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("level")
                        .then(Commands.literal("set")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, DndLeveling.MAX_LEVEL))
                                        .executes(context -> levelSet(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "level"))))))
                .then(Commands.literal("features")
                        .then(Commands.literal("list")
                                .executes(context -> featuresList(context.getSource()))))
                .then(Commands.literal("refresh")
                        .executes(context -> refresh(context.getSource())))
                .then(Commands.literal("use")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> use(context.getSource(),
                                        StringArgumentType.getString(context, "id")))))
        );
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            String subclass = level.getSubclassKey() == null || level.getSubclassKey().isEmpty()
                    ? "未选"
                    : level.getSubclassKey();
            source.sendSuccess(() -> Component.literal(
                    "职业：" + ClassRegistry.getDisplayName(level.getclassName())
                            + "  子职：" + subclass
                            + "  等级：" + level.getlevel()
                            + "  经验：" + level.getExp()
                            + "  熟练：+" + level.getproficiencybonus()), false);
        });
        return 1;
    }

    private static int xpAdd(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            int before = level.getlevel();
            level.setExp(DndLeveling.addExperience(level.getExp(), amount));
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal(
                    "经验 +" + amount + " → " + level.getExp() + "（等级 " + level.getlevel() + "）"), false);
            if (level.getlevel() > before) {
                source.sendSuccess(() -> Component.literal(
                        "升级！Lv " + before + " → " + level.getlevel()), true);
            }
        });
        return 1;
    }

    private static int xpSet(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            level.setExp(amount);
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal(
                    "经验设为 " + level.getExp() + "（等级 " + level.getlevel() + "）"), false);
        });
        return 1;
    }

    private static int levelSet(CommandSourceStack source, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            level.setlevel(value);
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal(
                    "等级设为 " + level.getlevel() + "（经验 " + level.getExp() + "）"), false);
        });
        return 1;
    }

    private static int featuresList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        java.util.List<FeatureDefinition> features = FeatureManager.activeFeatureList(player);
        if (features.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有生效特性。"), false);
        } else {
            for (FeatureDefinition feature : features) {
                source.sendSuccess(() -> Component.literal("- " + feature.id()
                        + "  [" + feature.owner().getPath()
                        + " Lv" + feature.level()
                        + " " + feature.kind() + "]"), false);
            }
        }
        return features.size();
    }

    private static int refresh(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterRefresh.refresh(player);
        source.sendSuccess(() -> Component.literal("已刷新特性与施法进度。"), false);
        return 1;
    }

    private static int use(CommandSourceStack source, String raw) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            source.sendFailure(Component.literal("无效的特性 id：" + raw));
            return 0;
        }
        FeatureManager.UseResult result = FeatureManager.use(player, id);
        switch (result) {
            case SUCCESS -> source.sendSuccess(() -> Component.literal(
                    "使用 " + id + "，剩余 " + FeatureManager.remainingUses(player, id) + " 次"), false);
            case UNKNOWN_FEATURE -> source.sendFailure(Component.literal("你尚未获得该特性：" + id));
            case NOT_ACTIVE -> source.sendFailure(Component.literal("该特性不是主动特性：" + id));
            case NO_USES -> source.sendFailure(Component.literal("次数不足：" + id));
        }
        return result == FeatureManager.UseResult.SUCCESS ? 1 : 0;
    }
}
