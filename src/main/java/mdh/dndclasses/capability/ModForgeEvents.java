package mdh.dndclasses.capability;

import mdh.dndclasses.Config;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.events.LevelUp;
import mdh.dndclasses.ability.Iability;
import mdh.dndclasses.ability.ability;
import mdh.dndclasses.level.DndLeveling;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.level.level;
import mdh.dndclasses.network.ServerStateSync;
import mdh.dndclasses.race.Irace;
import mdh.dndclasses.race.race;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModForgeEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider<Iskill> skillProvider = new Provider<>(ModCapabilities.SKILL_CAPABILITY, new skill());
            Provider<Iability> abilityProvider = new Provider<>(ModCapabilities.ABILITY_CAPABILITY, new ability());
            Provider<Ilevel> levelProvider = new Provider<>(ModCapabilities.LEVEL_CAPABILITY, new level());
            Provider<IDndSpell> spellProvider = new Provider<>(ModCapabilities.DND_SPELL_CAPABILITY, new Dndspell());
            Provider<Irace> raceProvider = new Provider<>(ModCapabilities.RACE_CAPABILITY, new race());

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

        if (created) {
            ServerStateSync.send(player);
        } else {
            ServerStateSync.sendPrompt(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player current = event.getEntity();

        original.reviveCaps();
        copyCapability(original, current, ModCapabilities.SKILL_CAPABILITY);
        copyCapability(original, current, ModCapabilities.ABILITY_CAPABILITY);
        copyCapability(original, current, ModCapabilities.LEVEL_CAPABILITY);
        copyCapability(original, current, ModCapabilities.DND_SPELL_CAPABILITY);
        copyCapability(original, current, ModCapabilities.RACE_CAPABILITY);
        original.invalidateCaps();
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
            int oldLevel = levelData.getlevel();
            int newExperience = DndLeveling.addExperience(levelData.getExp(), gainedExperience);
            levelData.setExp(newExperience);
            int newLevel = levelData.getlevel();

            player.sendSystemMessage(Component.literal("DND XP +" + gainedExperience + " (" + newExperience + ")").withStyle(ChatFormatting.GRAY));

            if (newLevel > oldLevel) {
                MinecraftForge.EVENT_BUS.post(new LevelUp(newLevel));
                player.sendSystemMessage(Component.literal("DND level up! Level " + oldLevel + " -> " + newLevel + ", proficiency +" + levelData.getproficiencybonus()).withStyle(ChatFormatting.GOLD));
            }

            ServerStateSync.send(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!event.wakeImmediately() && event.updateLevel()) {
            Player player = event.getEntity();
            player.getCapability(ModCapabilities.DND_SPELL_CAPABILITY).ifPresent(IDndSpell::reslots);
            player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(Iskill::relongskilluse);
        }
    }

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().isDay() && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.setDayTime(serverLevel.getDayTime() + 1000);
            player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(Iskill::reshortskilluse);
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            player.sendSystemMessage(Component.translatable("message.dndclasses.short_rest").withStyle(ChatFormatting.GREEN));
        }
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
