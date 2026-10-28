package mdh.dndclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.conditions.ConditionManager;
import mdh.dndclasses.data.ClassRegistry;
import mdh.dndclasses.data.SubclassSelection;
import mdh.dndclasses.events.DndConditionRemovedEvent;
import mdh.dndclasses.events.LevelUp;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.feature.FeatureManager;
import mdh.dndclasses.level.DndLeveling;
import mdh.dndclasses.network.ServerStateSync;
import mdh.dndclasses.saves.SaveQteManager;
import mdh.dndclasses.spells.SpellClassifiers;
import mdh.dndclasses.spells.SpellKind;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
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
                .then(Commands.literal("subclass")
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> subclassSet(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("list")
                                .executes(context -> subclassList(context.getSource()))))
                .then(Commands.literal("refresh")
                        .executes(context -> refresh(context.getSource())))
                .then(Commands.literal("save")
                        .then(Commands.argument("ability", StringArgumentType.word())
                                .then(Commands.argument("dc", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .executes(context -> save(context.getSource(),
                                                        StringArgumentType.getString(context, "ability"),
                                                        IntegerArgumentType.getInteger(context, "dc"),
                                                        StringArgumentType.getString(context, "mode")))))))
                .then(Commands.literal("condition")
                        .then(Commands.literal("apply")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(context -> conditionApply(context.getSource(),
                                                ResourceLocationArgument.getId(context, "id"), -1, -1))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                                .executes(context -> conditionApply(context.getSource(),
                                                        ResourceLocationArgument.getId(context, "id"),
                                                        IntegerArgumentType.getInteger(context, "seconds"), -1))
                                                .then(Commands.argument("dc", IntegerArgumentType.integer(1, 40))
                                                        .executes(context -> conditionApply(context.getSource(),
                                                                ResourceLocationArgument.getId(context, "id"),
                                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                                IntegerArgumentType.getInteger(context, "dc")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(context -> conditionRemove(context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")))))
                        .then(Commands.literal("list")
                                .executes(context -> conditionList(context.getSource()))))
                .then(Commands.literal("exhaustion")
                        .then(Commands.literal("set")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 6))
                                        .executes(context -> exhaustionSet(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "level"))))))
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
        // Player-usable alias used by the clickable subclass prompt (the /dnd tree is op-only).
        dispatcher.register(Commands.literal("dndsubclass")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> subclassSet(context.getSource(),
                                StringArgumentType.getString(context, "id")))));
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
                MinecraftForge.EVENT_BUS.post(new LevelUp(player, before, level.getlevel()));
                source.sendSuccess(() -> Component.literal(
                        "升级！Lv " + before + " → " + level.getlevel()), true);
            }
        });
        return 1;
    }

    private static int xpSet(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            int before = level.getlevel();
            level.setExp(amount);
            ServerStateSync.send(player);
            if (level.getlevel() > before) {
                MinecraftForge.EVENT_BUS.post(new LevelUp(player, before, level.getlevel()));
            }
            source.sendSuccess(() -> Component.literal(
                    "经验设为 " + level.getExp() + "（等级 " + level.getlevel() + "）"), false);
        });
        return 1;
    }

    private static int levelSet(CommandSourceStack source, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            int before = level.getlevel();
            level.setlevel(value);
            ServerStateSync.send(player);
            if (level.getlevel() > before) {
                MinecraftForge.EVENT_BUS.post(new LevelUp(player, before, level.getlevel()));
            }
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

    private static int save(CommandSourceStack source, String ability, int dc, String mode) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SaveQteManager.Mode parsed;
        try {
            parsed = SaveQteManager.Mode.valueOf(mode.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("模式必须是 timing / mash / hidden"));
            return 0;
        }
        SaveQteManager.request(player, ability.toLowerCase(java.util.Locale.ROOT), dc, parsed);
        source.sendSuccess(() -> Component.literal("豁免 " + ability + " DC " + dc + " (" + parsed + ")"), false);
        return 1;
    }

    private static int conditionApply(CommandSourceStack source, ResourceLocation id, int seconds, int dc) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int ticks = seconds > 0 ? seconds * 20 : -1;
        boolean ok = ConditionManager.apply(player, id, ticks, null, dc);
        if (ok) {
            source.sendSuccess(() -> Component.literal("已施加状态 " + id), false);
        } else {
            source.sendFailure(Component.literal("未知状态或免疫：" + id));
        }
        return ok ? 1 : 0;
    }

    private static int conditionRemove(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = ConditionManager.remove(player, id, DndConditionRemovedEvent.Reason.CURED);
        if (ok) {
            source.sendSuccess(() -> Component.literal("已移除状态 " + id), false);
        } else {
            source.sendFailure(Component.literal("该状态未生效：" + id));
        }
        return ok ? 1 : 0;
    }

    private static int conditionList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        java.util.List<net.minecraft.nbt.CompoundTag> list = ConditionManager.snapshot(player);
        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有状态。"), false);
            return 0;
        }
        for (int i = 0; i < list.size(); i++) {
            final net.minecraft.nbt.CompoundTag tag = list.get(i);
            source.sendSuccess(() -> Component.literal("- " + tag.getString("id") + " (" + tag.getInt("duration") + "t)"), false);
        }
        return list.size();
    }

    private static int exhaustionSet(CommandSourceStack source, int level) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ConditionManager.setExhaustion(player, level);
        source.sendSuccess(() -> Component.literal("力竭等级设为 " + level), false);
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

    private static int subclassSet(CommandSourceStack source, String rawId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SubclassSelection.Result result = SubclassSelection.pick(player, rawId);
        switch (result) {
            case SUCCESS -> source.sendSuccess(() -> Component.translatable(
                    "message.dndclasses.subclass.selected",
                    SubclassSelection.displayName(SubclassSelection.normalize(rawId))), false);
            case NO_CHARACTER -> source.sendFailure(Component.translatable("message.dndclasses.subclass.no_character"));
            case ALREADY_CHOSEN -> source.sendFailure(Component.translatable("message.dndclasses.subclass.already"));
            case TOO_EARLY -> source.sendFailure(Component.translatable("message.dndclasses.subclass.too_early"));
            case INVALID -> source.sendFailure(Component.translatable("message.dndclasses.subclass.invalid"));
        }
        return result == SubclassSelection.Result.SUCCESS ? 1 : 0;
    }

    private static int subclassList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            String current = level.getSubclassKey();
            if (current != null && !current.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("message.dndclasses.subclass.current",
                        SubclassSelection.displayName(current)), false);
                return;
            }
            java.util.List<String> ids = SubclassSelection.available(player);
            if (ids.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("message.dndclasses.subclass.none"), false);
                return;
            }
            for (String id : ids) {
                String key = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
                source.sendSuccess(() -> Component.literal("/dndsubclass " + key + "  ("
                        + SubclassSelection.displayName(id) + ")"), false);
            }
        });
        return 1;
    }
}
