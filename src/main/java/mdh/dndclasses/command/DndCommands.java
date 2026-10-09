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
import mdh.dndclasses.spells.SpellClassifiers;
import mdh.dndclasses.spells.SpellKind;
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
                .then(Commands.literal("spell")
                        .then(Commands.literal("list")
                                .executes(context -> spellList(context.getSource())))
                        .then(Commands.literal("learn")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(context -> spellLearn(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(context -> spellForget(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("prepare")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(context -> spellPrepare(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("unprepare")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(context -> spellUnprepare(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("clear")
                                .executes(context -> spellClear(context.getSource()))))
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

    private static int spellList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
            source.sendSuccess(() -> Component.literal("准备类型：" + spell.getPreparation()
                    + "  戏法上限：" + spell.getKnownCantripLimit()
                    + "  已知上限：" + spell.getKnownSpellLimit()
                    + "  准备上限：" + spell.getPreparedSpellLimit()), false);
            source.sendSuccess(() -> Component.literal("戏法：" + formatIds(spell.getKnownCantrips())), false);
            source.sendSuccess(() -> Component.literal("已知：" + formatIds(spell.getKnownSpells())), false);
            source.sendSuccess(() -> Component.literal("准备：" + formatIds(spell.getPreparedSpells())), false);
            source.sendSuccess(() -> Component.literal("可施放：" + formatIds(spell.getCastableSpells())), false);
        });
        return 1;
    }

    private static int spellLearn(CommandSourceStack source, String raw) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            source.sendFailure(Component.literal("无效的法术 id：" + raw));
            return 0;
        }
        SpellKind kind = SpellClassifiers.classify(id);
        if (kind == SpellKind.UNKNOWN) {
            source.sendFailure(Component.literal("无法判断法术类型（dndspell 未注册分类器或未知法术）：" + id));
            return 0;
        }
        boolean[] ok = {false};
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
            if (kind == SpellKind.CANTRIP) {
                ok[0] = spell.addKnownCantrip(id);
            } else {
                ok[0] = spell.addKnownSpell(id);
            }
        });
        if (ok[0]) {
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal("已学会：" + id), false);
        } else {
            source.sendFailure(Component.literal("学习失败（重复或已达上限）：" + id));
        }
        return ok[0] ? 1 : 0;
    }

    private static int spellForget(CommandSourceStack source, String raw) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            source.sendFailure(Component.literal("无效的法术 id：" + raw));
            return 0;
        }
        boolean[] ok = {false};
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
            boolean cantrip = spell.removeKnownCantrip(id);
            boolean leveled = spell.removeKnownSpell(id);
            ok[0] = cantrip || leveled;
        });
        if (ok[0]) {
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal("已遗忘：" + id), false);
        } else {
            source.sendFailure(Component.literal("遗忘失败（未学会）：" + id));
        }
        return ok[0] ? 1 : 0;
    }

    private static int spellPrepare(CommandSourceStack source, String raw) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            source.sendFailure(Component.literal("无效的法术 id：" + raw));
            return 0;
        }
        boolean[] ok = {false};
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> ok[0] = spell.prepareSpell(id));
        if (ok[0]) {
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal("已准备：" + id), false);
        } else {
            source.sendFailure(Component.literal("准备失败（重复或已达上限）：" + id));
        }
        return ok[0] ? 1 : 0;
    }

    private static int spellUnprepare(CommandSourceStack source, String raw) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            source.sendFailure(Component.literal("无效的法术 id：" + raw));
            return 0;
        }
        boolean[] ok = {false};
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> ok[0] = spell.unprepareSpell(id));
        if (ok[0]) {
            ServerStateSync.send(player);
            source.sendSuccess(() -> Component.literal("已取消准备：" + id), false);
        } else {
            source.sendFailure(Component.literal("取消准备失败（未准备）：" + id));
        }
        return ok[0] ? 1 : 0;
    }

    private static int spellClear(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> {
            spell.setKnownCantrips(java.util.List.of());
            spell.setKnownSpells(java.util.List.of());
            spell.setPreparedSpells(java.util.List.of());
        });
        ServerStateSync.send(player);
        source.sendSuccess(() -> Component.literal("已清空戏法/已知/准备清单。"), false);
        return 1;
    }

    private static String formatIds(java.util.Collection<ResourceLocation> ids) {
        if (ids.isEmpty()) {
            return "（空）";
        }
        StringBuilder builder = new StringBuilder();
        for (ResourceLocation id : ids) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(id);
        }
        return builder.toString();
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
