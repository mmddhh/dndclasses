package mdh.dndclasses.capability;

import mdh.dndclasses.Config;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.events.LevelUp;
import mdh.dndclasses.events.DndLongRestEvent;
import mdh.dndclasses.events.DndShortRestEvent;
import mdh.dndclasses.events.DndConcentrationBrokenEvent;
import mdh.dndclasses.conditions.ConditionManager;
import mdh.dndclasses.conditions.Conditions;
import mdh.dndclasses.conditions.IConditions;
import mdh.dndclasses.saves.SaveQteManager;
import mdh.dndclasses.ability.Iability;
import mdh.dndclasses.ability.ability;
import mdh.dndclasses.level.DndLeveling;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.level.level;
import mdh.dndclasses.network.ServerStateSync;
import mdh.dndclasses.race.Irace;
import mdh.dndclasses.race.race;
import mdh.dndclasses.spellcasting.SpellcastingManager;
import mdh.dndclasses.spells.Dndspell;
import mdh.dndclasses.spells.IDndSpell;
import mdh.dndclasses.spskill.Iskill;
import mdh.dndclasses.spskill.skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModForgeEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider<Iskill> skillProvider = new Provider<>(ModCapabilities.SKILL_CAPABILITY, new skill());
            Provider<Iability> abilityProvider = new Provider<>(ModCapabilities.ABILITY_CAPABILITY, new ability());
            Provider<Ilevel> levelProvider = new Provider<>(ModCapabilities.LEVEL_CAPABILITY, new level());
            Provider<IDndSpell> spellProvider = new Provider<>(ModCapabilities.DND_SPELL_CAPABILITY, new Dndspell());
            Provider<Irace> raceProvider = new Provider<>(ModCapabilities.RACE_CAPABILITY, new race());
            Provider<IConditions> conditionsProvider = new Provider<>(ModCapabilities.CONDITIONS_CAPABILITY, new Conditions());

            event.addCapability(
                    new ResourceLocation(Dndclasses.MODID, "skill"),
                    skillProvider
            );
            event.addListener(skillProvider::invalidate);
            event.addCapability(
                    new ResourceLocation(Dndclasses.MODID, "ability"),
                    abilityProvider
            );
            event.addListener(abilityProvider::invalidate);
            event.addCapability(
                    new ResourceLocation(Dndclasses.MODID, "level"),
                    levelProvider
            );
            event.addListener(levelProvider::invalidate);
            event.addCapability(
                    new ResourceLocation(Dndclasses.MODID, "dndspell"),
                    spellProvider
            );
            event.addListener(spellProvider::invalidate);
            event.addCapability(
                    new ResourceLocation(Dndclasses.MODID, "race"),
                    raceProvider
            );
            event.addListener(raceProvider::invalidate);
            event.addCapability(
                    new ResourceLocation(Dndclasses.MODID, "conditions"),
                    conditionsProvider
            );
            event.addListener(conditionsProvider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean created = player.getCapability(ModCapabilities.LEVEL_CAPABILITY)
                .map(Ilevel::hasCreatedCharacter)
                .orElse(false);

        awardModRecipes(player);

        if (created) {
            ServerStateSync.send(player);
        } else {
            ServerStateSync.sendPrompt(player);
        }
    }

    /**
     * Unlocks every D&amp;D crafting recipe (weapons, armor, orichalcum nugget/chain, ...) in the
     * recipe book on join.
     * <p>
     * The recipes ship as plain datapack JSON without vanilla-style recipe advancements, so
     * without this they would never show up in the recipe book; awarding them is idempotent.
     */
    private static void awardModRecipes(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        List<Recipe<?>> recipes = new ArrayList<>();
        for (CraftingRecipe recipe : player.getServer().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)) {
            Item result = recipe.getResultItem(player.level().registryAccess()).getItem();
            if (BuiltInRegistries.ITEM.getKey(result).getNamespace().equals(Dndclasses.MODID)) {
                recipes.add(recipe);
            }
        }
        if (!recipes.isEmpty()) {
            player.awardRecipes(recipes);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player current = event.getEntity();

        original.reviveCaps();
        if (event.isWasDeath() && !DEATH_BACKUP.containsKey(current.getUUID())) {
            // Keep a serialized snapshot so a respawn can restore the data even if the clone-time
            // capability copy is skipped for any reason.
            DEATH_BACKUP.put(current.getUUID(), snapshot(original));
            LOGGER.info("DND death snapshot stored for {}", current.getGameProfile().getName());
        }
        // Player persistent data (ForgeData) is not copied automatically for mods; carry it over so
        // the persistent death backup survives.
        current.getPersistentData().merge(original.getPersistentData().copy());
        copyCapability(original, current, ModCapabilities.SKILL_CAPABILITY);
        copyCapability(original, current, ModCapabilities.ABILITY_CAPABILITY);
        copyCapability(original, current, ModCapabilities.LEVEL_CAPABILITY);
        copyCapability(original, current, ModCapabilities.DND_SPELL_CAPABILITY);
        copyCapability(original, current, ModCapabilities.RACE_CAPABILITY);
        copyCapability(original, current, ModCapabilities.CONDITIONS_CAPABILITY);
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CapSnapshot backup = DEATH_BACKUP.remove(player.getUUID());
        if (backup == null) {
            backup = readPersistentBackup(player);
        }
        if (backup != null) {
            restore(player, backup);
            player.getPersistentData().remove(TAG_DEATH_BACKUP);
            LOGGER.info("DND death snapshot restored on respawn for {} class={}",
                    player.getGameProfile().getName(), currentClass(player));
        }
        // The client gets a fresh LocalPlayer on respawn; push the full state again so its
        // capabilities are re-hydrated immediately instead of appearing "empty" until relog.
        ServerStateSync.send(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        // Fallback for environments where PlayerRespawnEvent is not delivered: a dead player's
        // snapshot is restored as soon as the new player is alive again.
        CapSnapshot backup = DEATH_BACKUP.remove(player.getUUID());
        if (backup == null) {
            backup = readPersistentBackup(player);
        }
        if (backup != null && player.isAlive() && player.tickCount > 20) {
            restore(player, backup);
            player.getPersistentData().remove(TAG_DEATH_BACKUP);
            LOGGER.info("DND death snapshot restored on tick for {} class={}",
                    player.getGameProfile().getName(), currentClass(player));
            ServerStateSync.send(player);
        } else if (backup != null) {
            DEATH_BACKUP.put(player.getUUID(), backup);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerStateSync.send(player);
        }
    }

    private record CapSnapshot(CompoundTag skill, CompoundTag ability, CompoundTag level,
                               CompoundTag spell, CompoundTag race, CompoundTag conditions) {
    }

    private static final Map<UUID, CapSnapshot> DEATH_BACKUP = new HashMap<>();

    private static CapSnapshot snapshot(Player player) {
        return new CapSnapshot(nbt(player, ModCapabilities.SKILL_CAPABILITY),
                nbt(player, ModCapabilities.ABILITY_CAPABILITY),
                nbt(player, ModCapabilities.LEVEL_CAPABILITY),
                nbt(player, ModCapabilities.DND_SPELL_CAPABILITY),
                nbt(player, ModCapabilities.RACE_CAPABILITY),
                nbt(player, ModCapabilities.CONDITIONS_CAPABILITY));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompoundTag nbt(Player player, Capability<?> capability) {
        Object data = player.getCapability((Capability) capability).orElse(null);
        if (data instanceof INBTSerializable serializable) {
            Object tag = serializable.serializeNBT();
            if (tag instanceof CompoundTag compoundTag) {
                return compoundTag;
            }
        }
        return new CompoundTag();
    }

    private static void restore(Player player, CapSnapshot snapshot) {
        player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(data -> data.deserializeNBT(snapshot.skill()));
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(data -> data.deserializeNBT(snapshot.ability()));
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(data -> data.deserializeNBT(snapshot.level()));
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(data -> data.deserializeNBT(snapshot.spell()));
        player.getCapability(ModCapabilities.RACE_CAPABILITY).ifPresent(data -> data.deserializeNBT(snapshot.race()));
        player.getCapability(ModCapabilities.CONDITIONS_CAPABILITY).ifPresent(data -> data.deserializeNBT(snapshot.conditions()));
    }

    private static final String TAG_DEATH_BACKUP = "DndDeathBackup";

    private static void writePersistentBackup(ServerPlayer player, CapSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.put("skill", snapshot.skill());
        tag.put("ability", snapshot.ability());
        tag.put("level", snapshot.level());
        tag.put("spell", snapshot.spell());
        tag.put("race", snapshot.race());
        tag.put("conditions", snapshot.conditions());
        player.getPersistentData().put(TAG_DEATH_BACKUP, tag);
    }

    private static CapSnapshot readPersistentBackup(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG_DEATH_BACKUP);
        if (tag.isEmpty()) {
            return null;
        }
        return new CapSnapshot(tag.getCompound("skill"), tag.getCompound("ability"),
                tag.getCompound("level"), tag.getCompound("spell"),
                tag.getCompound("race"), tag.getCompound("conditions"));
    }

    private static String currentClass(ServerPlayer player) {
        return player.getCapability(ModCapabilities.LEVEL_CAPABILITY)
                .map(Ilevel::getclassName).orElse("");
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity killedEntity = event.getEntity();
        if (killedEntity instanceof Player) {
            return;
        }

        boolean created = player.getCapability(ModCapabilities.LEVEL_CAPABILITY)
                .map(Ilevel::hasCreatedCharacter)
                .orElse(false);
        if (!created) {
            return;
        }

        int gainedExperience = getDndExperience(killedEntity);
        if (gainedExperience <= 0) {
            return;
        }

        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(levelData -> {
            int newExperience = DndLeveling.addExperience(levelData.getExp(), gainedExperience);
            levelData.setExp(newExperience);
            player.sendSystemMessage(Component.literal("DND XP +" + gainedExperience + " (" + newExperience + ")")
                    .withStyle(ChatFormatting.GRAY));
            ServerStateSync.send(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        // wakeImmediately is false for natural wakes (the night-skip wakeUpAllPlayers uses
        // (false,false), the dawn time-check uses (false,true)) and true for getting up early /
        // taking damage / disconnecting. So !wakeImmediately() means "completed a long rest".
        if (!event.wakeImmediately()) {
            Player player = event.getEntity();
            player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(IDndSpell::reslots);
            player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(Iskill::relongskilluse);
            if (player instanceof ServerPlayer serverPlayer) {
                SaveQteManager.breakConcentration(serverPlayer, DndConcentrationBrokenEvent.Reason.OTHER);
                ConditionManager.decreaseExhaustion(serverPlayer);
                MinecraftForge.EVENT_BUS.post(new DndLongRestEvent(serverPlayer));
                mdh.dndclasses.data.LevelUpManager.onLongRest(serverPlayer);
                ServerStateSync.send(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().isDay() && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.setDayTime(serverLevel.getDayTime() + 1000);
            player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(Iskill::reshortskilluse);
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftForge.EVENT_BUS.post(new DndShortRestEvent(serverPlayer));
                SpellcastingManager.onShortRest(serverPlayer);
                ServerStateSync.send(serverPlayer);
            }
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            player.sendSystemMessage(Component.translatable("message.dndclasses.short_rest").withStyle(ChatFormatting.GREEN));
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }
        boolean[] concentrating = {false};
        player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(spell -> concentrating[0] = spell.isConcentrating());
        if (!concentrating[0]) {
            return;
        }
        int dc = Math.max(10, (int) Math.floor(event.getAmount() / 2.0));
        SaveQteManager.request(player, "con", dc, SaveQteManager.Mode.TIMING);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SaveQteManager.breakConcentration(player, DndConcentrationBrokenEvent.Reason.DEATH);
            ConditionManager.clearOnDeath(player);
            // Snapshot after the death cleanup (conditions/concentration should not come back), but
            // before the death screen replaces the player; this does not depend on
            // PlayerEvent.Clone being delivered.
            DEATH_BACKUP.put(player.getUUID(), snapshot(player));
            writePersistentBackup(player, DEATH_BACKUP.get(player.getUUID()));
            LOGGER.info("DND death snapshot (death event) for {} class={}",
                    player.getGameProfile().getName(), currentClass(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getEntity().getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(IDndSpell::clearConcentration);
    }

    private static <T extends INBTSerializable<CompoundTag>> void copyCapability(Player original, Player current, Capability<T> capability) {
        original.getCapability(capability).ifPresent(oldData -> current.getCapability(capability).ifPresent(newData -> newData.deserializeNBT(oldData.serializeNBT())));
    }

    private static int getDndExperience(LivingEntity killedEntity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killedEntity.getType());
        Integer configuredExperience = Config.entityExperienceOverrides.get(entityId);
        if (configuredExperience != null) {
            return configuredExperience;
        }

        if (!(killedEntity instanceof Monster)) {
            return 0;
        }

        int healthBasedExperience = Math.round(killedEntity.getMaxHealth() * Config.defaultHostileXpPerHealth);
        return Math.max(Config.defaultHostileMinXp, healthBasedExperience);
    }
}
