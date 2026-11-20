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
import mdh.dndclasses.worldgen.NpcProfile;
import mdh.dndclasses.worldgen.mc.SimulationAdapter;
import mdh.dndclasses.worldgen.WorldPlan;
import mdh.dndclasses.worldgen.WorldPlanGenerator;
import mdh.dndclasses.worldgen.WorldPlanVerifier;
import mdh.dndclasses.worldgen.mc.NpcSpawner;
import mdh.dndclasses.worldgen.mc.RealHabitabilitySampler;
import mdh.dndclasses.worldgen.mc.SettlementBuilder;
import mdh.dndclasses.worldgen.mc.SettlementSiteSampler;
import mdh.dndclasses.worldgen.mc.TerrainSiteSelector;
import mdh.dndclasses.worldgen.mc.RealRoadTerrain;
import mdh.dndclasses.worldgen.mc.RoadPainter;
import mdh.dndclasses.worldgen.road.RoadNetwork;
import mdh.dndclasses.worldgen.road.RoadPlanner;
import mdh.dndclasses.worldgen.sim.CivilizationSimulator;
import mdh.dndclasses.worldgen.sim.SimulationResult;
import mdh.dndclasses.worldgen.mc.WorldPlanSavedData;
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
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        dispatcher.register(Commands.literal("worldplan")
                .requires(source -> source.hasPermission(2))
                .executes(context -> worldPlan(context.getSource(), false))
                .then(Commands.literal("json")
                        .executes(context -> worldPlan(context.getSource(), true)))
                .then(Commands.literal("verify")
                        .executes(context -> worldPlanVerify(context.getSource())))
                .then(Commands.literal("regen")
                        .executes(context -> worldPlanRegen(context.getSource())))
                .then(Commands.literal("terrain")
                        .executes(context -> worldPlanTerrain(context.getSource())))
                .then(Commands.literal("build")
                        .then(Commands.argument("settlement", IntegerArgumentType.integer(0, 10000))
                                .executes(context -> worldPlanBuild(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "settlement")))))
                .then(Commands.literal("tp")
                        .then(Commands.argument("settlement", IntegerArgumentType.integer(0, 10000))
                                .executes(context -> worldPlanTeleport(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "settlement")))))
                .then(Commands.literal("info")
                        .then(Commands.argument("settlement", IntegerArgumentType.integer(0, 10000))
                                .executes(context -> worldPlanInfo(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "settlement")))))
                .then(Commands.literal("selftest")
                        .executes(context -> worldPlanSelftest(context.getSource())))
                .then(Commands.literal("resite")
                        .executes(context -> worldPlanResite(context.getSource())))
                .then(Commands.literal("simulate")
                        .executes(context -> worldPlanSimulate(context.getSource())))
                .then(Commands.literal("adopt")
                        .executes(context -> worldPlanAdopt(context.getSource())))
                .then(Commands.literal("roads")
                        .executes(context -> worldPlanRoads(context.getSource())))
                .then(Commands.literal("paint")
                        .executes(context -> worldPlanPaint(context.getSource())))
                .then(Commands.literal("dimension")
                        .executes(context -> worldPlanDimension(context.getSource())))
                .then(Commands.literal("map")
                        .executes(context -> worldPlanMap(context.getSource()))));

        dispatcher.register(Commands.literal("npc")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(context -> npcList(context.getSource())))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("settlement", IntegerArgumentType.integer(0, 10000))
                                .executes(context -> npcSpawn(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "settlement"))))));
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

    private static int worldPlan(CommandSourceStack source, boolean writeJson) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        source.sendSuccess(() -> Component.literal(
                "WorldPlan seed=" + plan.seed()
                        + "  国家=" + plan.nations().size()
                        + "  聚落=" + plan.settlements().size()
                        + "  事件=" + plan.events().size()), false);
        for (int i = 0; i < Math.min(5, plan.settlements().size()); i++) {
            WorldPlan.Settlement settlement = plan.settlements().get(i);
            WorldPlan.Nation nation = plan.nations().get(settlement.nationId());
            source.sendSuccess(() -> Component.literal(
                    "[" + settlement.id() + "] " + settlement.name()
                            + " (" + nation.name() + ", " + settlement.population() + "人, "
                            + describeCulture(settlement.culture()) + ")"), false);
        }
        for (int i = Math.max(0, plan.events().size() - 3); i < plan.events().size(); i++) {
            WorldPlan.HistoryEvent event = plan.events().get(i);
            source.sendSuccess(() -> Component.literal(
                    "Y" + event.year() + " [" + event.type() + "] " + event.summary()), false);
        }
        if (writeJson) {
            Path directory = FMLPaths.CONFIGDIR.get().resolve("dndclasses");
            Path file = directory.resolve("worldplan-" + plan.seed() + ".json");
            try {
                Files.createDirectories(directory);
                Files.writeString(file, plan.toJson().toString());
                source.sendSuccess(() -> Component.literal("已写入 " + file), false);
            } catch (IOException exception) {
                source.sendFailure(Component.literal("写入失败: " + exception.getMessage()));
                return 0;
            }
        }
        return 1;
    }

    private static int worldPlanVerify(CommandSourceStack source) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        WorldPlanVerifier.Result result = WorldPlanVerifier.verify(plan);
        if (result.ok()) {
            source.sendSuccess(() -> Component.literal("WorldPlan 校验通过。"), false);
        } else {
            source.sendFailure(Component.literal("WorldPlan 校验失败，共 " + result.errors().size() + " 项："));
            result.errors().stream().limit(10).forEach(error ->
                    source.sendFailure(Component.literal(" - " + error)));
        }
        result.warnings().stream().limit(5).forEach(warning ->
                source.sendSuccess(() -> Component.literal("警告: " + warning), false));
        return result.ok() ? 1 : 0;
    }

    private static int worldPlanRegen(CommandSourceStack source) {
        WorldPlanSavedData data = WorldPlanSavedData.get(source.getLevel());
        WorldPlan regenerated = WorldPlanGenerator.generate(source.getLevel().getSeed());
        data.replace(regenerated);
        mdh.dndclasses.worldgen.mc.WorldPlanCache.set(regenerated);
        source.sendSuccess(() -> Component.literal("WorldPlan 已按当前种子重新生成并保存。"), false);
        return 1;
    }

    private static int worldPlanTerrain(CommandSourceStack source) {
        WorldPlanSavedData data = WorldPlanSavedData.get(source.getLevel());
        WorldPlan adjusted = SettlementSiteSampler.sample(source.getLevel(), data.plan());
        data.replace(adjusted);
        mdh.dndclasses.worldgen.mc.WorldPlanCache.set(adjusted);
        source.sendSuccess(() -> Component.literal("已按实际地形校正聚落坐标并保存。"), false);
        return 1;
    }

    private static int worldPlanResite(CommandSourceStack source) {
        WorldPlanSavedData data = WorldPlanSavedData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal(
                "正在寻找陆地锚点并重新分布聚落，可能触发区块生成，请稍候……"), false);
        WorldPlan adjusted = TerrainSiteSelector.resite(source.getLevel(), data.plan());
        data.replace(adjusted);
        mdh.dndclasses.worldgen.mc.WorldPlanCache.set(adjusted);
        source.sendSuccess(() -> Component.literal("聚落已重新分布到陆地附近并保存。"), false);
        return 1;
    }

    private static int worldPlanDimension(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key =
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new ResourceLocation("dndclasses", "dnd_overworld"));
        net.minecraft.server.level.ServerLevel level = source.getServer().getLevel(key);
        if (level == null) {
            source.sendFailure(Component.literal(
                    "测试维度未加载：检查 data/dndclasses/dimension/dnd_overworld.json"));
            return 0;
        }
        mdh.dndclasses.worldgen.terrain.TerrainDensity density =
                new mdh.dndclasses.worldgen.terrain.TerrainDensity(
                        mdh.dndclasses.worldgen.terrain.WorldTopology.standard(),
                        level.getSeed());
        int[] land = findLand(density, 0, 0, 12_000, 256);
        int x = land[0];
        int z = land[1];
        int landY = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        level.getWorldBorder().setCenter(0.0D, 0.0D);
        level.getWorldBorder().setSize(62_720.0D);
        player.teleportTo(level, x + 0.5D, landY + 1.0D, z + 0.5D,
                player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal(
                "已进入 dndclasses:dnd_overworld，落点 " + x + ", " + z + "。"), false);
        return 1;
    }

    private static int worldPlanMap(CommandSourceStack source) {
        long seed = source.getLevel().getSeed();
        mdh.dndclasses.worldgen.terrain.TerrainGrid grid =
                mdh.dndclasses.worldgen.terrain.HabitabilitySampler.sample(
                        mdh.dndclasses.worldgen.terrain.WorldTopology.standard(), seed);
        Path directory = FMLPaths.CONFIGDIR.get().resolve("dndclasses").resolve("maps");
        try {
            grid.writeImages(directory);
        } catch (IOException exception) {
            source.sendFailure(Component.literal("写地形图失败: " + exception.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("地形图已写入 " + directory
                + "（seed=" + seed + "）"), false);
        return 1;
    }

    private static int[] findLand(mdh.dndclasses.worldgen.terrain.TerrainDensity density,
                                  int centerX, int centerZ, int maxRadius, int step) {
        for (int radius = 0; radius <= maxRadius; radius += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (density.surfaceHeight(x, z) > 73) {
                        return new int[]{x, z};
                    }
                }
            }
        }
        return new int[]{0, 0};
    }

    private static int worldPlanPaint(CommandSourceStack source) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        source.sendSuccess(() -> Component.literal(
                "正在铺道路并建造聚落测试塔，可能持续较久……"), false);
        RoadNetwork network = RoadPlanner.plan(plan, new RealRoadTerrain(source.getLevel()));
        int roads = RoadPainter.paintRoads(source.getLevel(), network);
        int settlements = RoadPainter.paintSettlements(source.getLevel(), plan);
        source.sendSuccess(() -> Component.literal("已铺设 " + roads + " 个道路方块，建造 "
                + settlements + " 个聚落测试塔。"), false);
        return 1;
    }

    private static int worldPlanRoads(CommandSourceStack source) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        source.sendSuccess(() -> Component.literal(
                "正在规划道路网络，A* 可能触发大量地形采样，请稍候……"), false);
        RoadNetwork network = RoadPlanner.plan(plan, new RealRoadTerrain(source.getLevel()));
        Path directory = FMLPaths.CONFIGDIR.get().resolve("dndclasses");
        Path file = directory.resolve("roads-" + plan.seed() + ".json");
        Path mapFile = directory.resolve("roads-map-" + plan.seed() + ".txt");
        Path svgFile = directory.resolve("roads-map-" + plan.seed() + ".svg");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, network.prettyJson());
            Files.write(mapFile, network.debugMap(plan, new RealRoadTerrain(source.getLevel())));
            Files.writeString(svgFile, network.toSvg(plan, new RealRoadTerrain(source.getLevel())));
        } catch (IOException exception) {
            source.sendFailure(Component.literal("写入道路规划失败: " + exception.getMessage()));
            return 0;
        }
        long highways = network.roads().stream()
                .filter(road -> road.tier() == RoadNetwork.Tier.HIGHWAY).count();
        long roads = network.roads().stream()
                .filter(road -> road.tier() == RoadNetwork.Tier.ROAD).count();
        long paths = network.roads().stream()
                .filter(road -> road.tier() == RoadNetwork.Tier.PATH).count();
        source.sendSuccess(() -> Component.literal("道路：王道 " + highways + "，驰道 " + roads
                + "，乡道 " + paths + "；边境哨塔 " + network.borderPosts().size()
                + "；失败路线 " + network.failedRoutes().size()), false);
        source.sendSuccess(() -> Component.literal("道路规划已写入 " + file), false);
        source.sendSuccess(() -> Component.literal("可视化地图已写入 " + mapFile), false);
        source.sendSuccess(() -> Component.literal("SVG 地图已写入 " + svgFile), false);
        return 1;
    }

    private static int worldPlanAdopt(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "正在采样地形并采用文明演算结果……"), false);
        mdh.dndclasses.worldgen.sim.HabitabilityMap map =
                RealHabitabilitySampler.sample(source.getLevel());
        SimulationResult simulation = CivilizationSimulator.run(map);
        WorldPlan plan = SimulationAdapter.adapt(simulation);
        // The simulation works on 256-block cells; snap each settlement onto real dry ground
        // before persisting it.
        WorldPlan sampled = SettlementSiteSampler.sample(source.getLevel(), plan);
        WorldPlan adopted = SettlementSiteSampler.filterUnplaceable(source.getLevel(), sampled);
        WorldPlanSavedData data = WorldPlanSavedData.get(source.getLevel());
        data.replace(adopted);
        mdh.dndclasses.worldgen.mc.WorldPlanCache.set(adopted);
        source.sendSuccess(() -> Component.literal(
                "已采用演算结果：国家 " + adopted.nations().size()
                        + "，聚落 " + adopted.settlements().size()
                        + "（含首都 " + adopted.nations().size() + "）"
                        + "，事件 " + adopted.events().size()), false);
        WorldPlanVerifier.Result verify = WorldPlanVerifier.verify(adopted);
        if (!verify.ok()) {
            source.sendFailure(Component.literal("采用后的计划校验失败，共 "
                    + verify.errors().size() + " 项："));
            verify.errors().stream().limit(5).forEach(error ->
                    source.sendFailure(Component.literal(" - " + error)));
        }
        Path directory = FMLPaths.CONFIGDIR.get().resolve("dndclasses");
        Path file = directory.resolve("civilization-" + adopted.seed() + ".json");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, simulation.prettyJson());
            source.sendSuccess(() -> Component.literal("演算快照已写入 " + file), false);
        } catch (IOException exception) {
            source.sendFailure(Component.literal("写入演算快照失败: " + exception.getMessage()));
        }
        return 1;
    }

    private static int worldPlanSimulate(CommandSourceStack source) {
        long seed = source.getLevel().getSeed();
        mdh.dndclasses.worldgen.sim.HabitabilityMap map =
                RealHabitabilitySampler.sample(source.getLevel());
        SimulationResult result = CivilizationSimulator.run(map);
        Path directory = FMLPaths.CONFIGDIR.get().resolve("dndclasses");
        Path file = directory.resolve("civilization-" + seed + ".json");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, result.prettyJson());
        } catch (IOException exception) {
            source.sendFailure(Component.literal("写入文明演算失败: " + exception.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "文明演算：国家 " + result.nations().size()
                        + "，聚落 " + result.settlements().size()
                        + "，事件 " + result.events().size()), false);
        result.nations().stream().limit(6).forEach(nation ->
                source.sendSuccess(() -> Component.literal(
                        "[" + nation.id() + "] " + nation.name() + "（" + raceName(nation.race())
                                + "）领土 " + nation.cells() + " 格 · 首都 "
                                + nation.capitalX() + "," + nation.capitalZ()
                                + " · " + describeCulture(nation.culture())), false));
        result.events().stream().skip(Math.max(0, result.events().size() - 5)).forEach(event ->
                source.sendSuccess(() -> Component.literal(
                        "Y" + event.year() + " [" + event.type() + "] " + event.summary()), false));
        source.sendSuccess(() -> Component.literal("完整结果已写入 " + file), false);
        return 1;
    }

    private static int worldPlanBuild(CommandSourceStack source, int settlementId) {
        WorldPlan.Settlement settlement = WorldPlanSavedData.get(source.getLevel()).plan().settlements()
                .stream().filter(candidate -> candidate.id() == settlementId).findFirst().orElse(null);
        if (settlement == null) {
            source.sendFailure(Component.literal("没有 id=" + settlementId + " 的聚落。"));
            return 0;
        }
        if (mdh.dndclasses.worldgen.mc.WorldPlanCache.isBuilt(settlementId)) {
            source.sendSuccess(() -> Component.literal("该聚落已经生成过测试建筑。"), false);
            return 1;
        }
        int placed = SettlementBuilder.build(source.getLevel(), settlement);
        if (placed <= 0) {
            source.sendFailure(Component.literal("建筑放置失败（地形高度无效）。"));
            return 0;
        }
        mdh.dndclasses.worldgen.mc.WorldPlanCache.markBuilt(settlementId);
        source.sendSuccess(() -> Component.literal(
                "在 " + settlement.name() + "（" + settlement.x() + ", " + settlement.z()
                        + "）放置了测试建筑，按当地地形选材，共 " + placed + " 个方块。"), false);
        return 1;
    }

    private static int worldPlanTeleport(CommandSourceStack source, int settlementId)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WorldPlanSavedData data = WorldPlanSavedData.get(source.getLevel());
        WorldPlan plan = data.plan();
        WorldPlan.Settlement settlement = findSettlement(plan, settlementId);
        if (settlement == null) {
            source.sendFailure(Component.literal("没有 id=" + settlementId + " 的聚落。"));
            return 0;
        }
        SettlementBuilder.Site site = SettlementBuilder.findSite(source.getLevel(),
                settlement.x(), settlement.z());
        if (site == null) {
            // The plan may still point at ocean. Correct this settlement against real terrain,
            // persist it and refresh the chunk-thread cache before teleporting.
            WorldPlan adjusted = SettlementSiteSampler.relocate(source.getLevel(), plan,
                    settlementId, 512);
            if (adjusted != plan) {
                data.replace(adjusted);
                mdh.dndclasses.worldgen.mc.WorldPlanCache.set(adjusted);
                plan = adjusted;
                settlement = findSettlement(plan, settlementId);
            }
        }
        net.minecraft.core.BlockPos target = site == null ? null
                : new net.minecraft.core.BlockPos(site.x(), site.y(), site.z());
        if (target == null && settlement != null) {
            target = SettlementSiteSampler.findSite(source.getLevel(),
                    settlement.x(), settlement.z(), 512);
        }
        if (target == null && settlement != null) {
            SettlementBuilder.Site water = SettlementBuilder.findWaterSite(source.getLevel(),
                    settlement.x(), settlement.z());
            if (water != null) {
                target = new net.minecraft.core.BlockPos(water.x(), water.y(), water.z());
            }
        }
        if (target == null && settlement != null) {
            // Ocean-only seed: use a sea-level platform coordinate rather than the world spawn.
            int seaY = Math.max(63, source.getLevel().getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    settlement.x(), settlement.z()));
            target = new net.minecraft.core.BlockPos(settlement.x(), seaY, settlement.z());
        }
        if (target == null || settlement == null) {
            // Only reachable when the settlement id itself is invalid.
            target = source.getLevel().getSharedSpawnPos();
        }
        String teleportName = settlement == null ? "未知聚落" : settlement.name();
        player.teleportTo(source.getLevel(), target.getX() + 0.5D, target.getY() + 1.0D,
                target.getZ() + 0.5D, player.getYRot(), player.getXRot());
        if (settlement != null) {
            // The terrain search may have generated the target chunk before the plan cache was
            // updated, so the worldgen feature can miss it. Build the watchtower explicitly here,
            // but never twice: two builds would stack a tower on top of its own roof.
            if (!mdh.dndclasses.worldgen.mc.WorldPlanCache.isBuilt(settlement.id())) {
                if (SettlementBuilder.build(source.getLevel(), settlement) > 0) {
                    mdh.dndclasses.worldgen.mc.WorldPlanCache.markBuilt(settlement.id());
                }
            }
        }
        source.sendSuccess(() -> Component.literal("已传送到 " + teleportName
                + " 的地表。"), false);
        return 1;
    }

    private static WorldPlan.Settlement findSettlement(WorldPlan plan, int id) {
        return plan.settlements().stream()
                .filter(candidate -> candidate.id() == id).findFirst().orElse(null);
    }

    private static int worldPlanInfo(CommandSourceStack source, int settlementId) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        WorldPlan.Settlement settlement = plan.settlements().stream()
                .filter(candidate -> candidate.id() == settlementId).findFirst().orElse(null);
        if (settlement == null) {
            source.sendFailure(Component.literal("没有 id=" + settlementId + " 的聚落。"));
            return 0;
        }
        WorldPlan.Nation nation = plan.nations().get(settlement.nationId());
        source.sendSuccess(() -> Component.literal(
                "[" + settlement.id() + "] " + settlement.name()
                        + " · " + nation.name() + " (" + raceName(nation.race()) + ")"
                        + " · 人口 " + settlement.population()), false);
        source.sendSuccess(() -> Component.literal("坐标: " + settlement.x() + ", " + settlement.z()
                + " · 国家文化: " + describeCulture(nation.culture())), false);
        source.sendSuccess(() -> Component.literal("聚落文化: " + describeCulture(settlement.culture())
                + " · 文化偏好: " + materialNames(settlement.materials())), false);
        String local = SettlementBuilder.materialAt(source.getLevel(), settlement);
        source.sendSuccess(() -> Component.literal("实际建材: "
                + (local == null ? "附近没有干地" : materialName(local))), false);
        return 1;
    }

    private static int worldPlanSelftest(CommandSourceStack source) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        int passed = 0;
        int total = 0;

        total++;
        boolean counts = !plan.nations().isEmpty() && !plan.settlements().isEmpty()
                && !plan.events().isEmpty();
        source.sendSuccess(() -> Component.literal((counts ? "[通过] " : "[失败] ")
                + "计划规模：国家 " + plan.nations().size() + "，聚落 "
                + plan.settlements().size() + "，事件 " + plan.events().size()), false);
        if (counts) {
            passed++;
        }

        total++;
        WorldPlanVerifier.Result verify = WorldPlanVerifier.verify(plan);
        source.sendSuccess(() -> Component.literal((verify.ok() ? "[通过] " : "[失败] ")
                + "计划校验" + (verify.ok() ? "" : "：" + verify.errors().size() + " 项错误")), false);
        if (verify.ok()) {
            passed++;
        }

        total++;
        boolean cacheOk = true;
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(
                    settlement.x() >> 4, settlement.z() >> 4);
            WorldPlan.Settlement cached = mdh.dndclasses.worldgen.mc.WorldPlanCache.settlementAt(chunk);
            if (cached == null || cached.id() != settlement.id()) {
                cacheOk = false;
                break;
            }
        }
        boolean finalCacheOk = cacheOk;
        source.sendSuccess(() -> Component.literal((finalCacheOk ? "[通过] " : "[失败] ")
                + "区块缓存与计划一致"), false);
        if (cacheOk) {
            passed++;
        }

        total++;
        boolean featureRegistered = net.minecraft.core.registries.BuiltInRegistries.FEATURE
                .containsKey(new ResourceLocation("dndclasses", "planned_ruin"));
        source.sendSuccess(() -> Component.literal((featureRegistered ? "[通过] " : "[失败] ")
                + "planned_ruin Feature 已注册"), false);
        if (featureRegistered) {
            passed++;
        }

        total++;
        boolean dataOk = source.getServer().getResourceManager()
                .getResource(new ResourceLocation("dndclasses",
                        "worldgen/placed_feature/planned_ruin.json")).isPresent()
                && source.getServer().getResourceManager()
                .getResource(new ResourceLocation("dndclasses",
                        "worldgen/configured_feature/planned_ruin.json")).isPresent();
        source.sendSuccess(() -> Component.literal((dataOk ? "[通过] " : "[失败] ")
                + "Feature 数据文件存在"), false);
        if (dataOk) {
            passed++;
        }

        int finalPassed = passed;
        int finalTotal = total;
        source.sendSuccess(() -> Component.literal("M1 selftest: " + finalPassed + "/" + finalTotal
                + (finalPassed == finalTotal ? " 通过" : " 未通过")), false);
        return finalPassed == finalTotal ? 1 : 0;
    }

    private static int npcList(CommandSourceStack source) {
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        for (int i = 0; i < Math.min(20, plan.settlements().size()); i++) {
            WorldPlan.Settlement settlement = plan.settlements().get(i);
            WorldPlan.Nation nation = plan.nations().get(settlement.nationId());
            source.sendSuccess(() -> Component.literal(
                    "[" + settlement.id() + "] " + settlement.name() + " · " + raceName(nation.race())
                            + " · " + nation.name()), false);
        }
        return 1;
    }

    private static int npcSpawn(CommandSourceStack source, int settlementId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WorldPlan plan = WorldPlanSavedData.get(source.getLevel()).plan();
        WorldPlan.Settlement settlement = plan.settlements().stream()
                .filter(candidate -> candidate.id() == settlementId)
                .findFirst()
                .orElse(null);
        if (settlement == null) {
            source.sendFailure(Component.literal("没有 id=" + settlementId + " 的聚落。"));
            return 0;
        }
        NpcProfile profile = WorldPlanGenerator.npcFor(plan, settlement, settlementId);
        if (NpcSpawner.spawn(player, profile) == null) {
            source.sendFailure(Component.literal("NPC 生成失败。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "生成了 " + profile.name() + "（" + raceName(profile.race()) + " "
                        + roleName(profile.role())
                        + "，聚落 " + settlement.name() + "）"), false);
        return 1;
    }

    private static String describeCulture(String culture) {
        if (culture == null || culture.isBlank()) {
            return "无";
        }
        StringBuilder out = new StringBuilder();
        for (String part : culture.split(";")) {
            int split = part.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String key = part.substring(0, split);
            String value = part.substring(split + 1);
            String label = switch (key) {
                case "roof" -> "屋顶";
                case "window" -> "窗户";
                case "motif" -> "纹样";
                case "trim" -> "装饰";
                default -> key;
            };
            String translated = switch (key + "=" + value) {
                case "roof=steep" -> "尖顶";
                case "roof=flat" -> "平顶";
                case "roof=domed" -> "穹顶";
                case "roof=gabled" -> "人字顶";
                case "roof=spiral" -> "螺旋顶";
                case "window=small" -> "小窗";
                case "window=tall" -> "高窗";
                case "window=arched" -> "拱窗";
                case "window=round" -> "圆窗";
                case "motif=rune" -> "符文";
                case "motif=vine" -> "藤蔓";
                case "motif=star" -> "星辰";
                case "motif=dragon" -> "龙";
                case "motif=hammer" -> "锤";
                case "motif=wave" -> "波浪";
                case "trim=carved" -> "雕刻";
                case "trim=painted" -> "彩绘";
                case "trim=plain" -> "素面";
                case "trim=gilded" -> "镀金";
                default -> value;
            };
            if (!out.isEmpty()) {
                out.append("，");
            }
            out.append(label).append(' ').append(translated);
        }
        return out.isEmpty() ? "无" : out.toString();
    }

    private static String materialNames(java.util.List<String> materials) {
        if (materials == null || materials.isEmpty()) {
            return "无";
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String material : materials) {
            names.add(materialName(material));
        }
        return String.join("，", names);
    }

    private static String materialName(String material) {
        return switch (material == null ? "" : material) {
            case "oak" -> "橡木";
            case "spruce" -> "云杉";
            case "stone" -> "石料";
            case "deepslate" -> "深板岩";
            case "sandstone" -> "砂岩";
            case "copper" -> "铜";
            default -> material == null ? "未知" : material;
        };
    }

    private static String raceName(String race) {
        return switch (race == null ? "" : race) {
            case "human" -> "人类";
            case "elf" -> "精灵";
            case "dwarf" -> "矮人";
            case "orc" -> "兽人";
            case "halfling" -> "半身人";
            case "dragonborn" -> "龙裔";
            default -> race == null ? "未知" : race;
        };
    }

    private static String roleName(String role) {
        return switch (role == null ? "" : role) {
            case "merchant" -> "商人";
            case "guard" -> "守卫";
            case "scholar" -> "学者";
            case "priest" -> "祭司";
            default -> role == null ? "平民" : role;
        };
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
