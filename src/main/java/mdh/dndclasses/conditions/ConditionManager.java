package mdh.dndclasses.conditions;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.events.DndConditionAppliedEvent;
import mdh.dndclasses.events.DndConditionRemovedEvent;
import mdh.dndclasses.network.ServerStateSync;
import mdh.dndclasses.saves.Saves;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Applies/removes data-driven D&amp;D conditions and enforces their real-time behaviour.
 * Periodic saves use a real d20 + the relevant save bonus against the stored DC.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConditionManager {

    private static final UUID EXHAUSTION_SPEED = UUID.fromString("5c1f9a1e-3d2b-4a7e-9f1c-1a2b3c4d5e6f");
    private static final UUID EXHAUSTION_HEALTH = UUID.fromString("7e2d4b3a-6c8f-4d1e-8b9a-0f1e2d3c4b5a");

    private static final Map<UUID, Set<ResourceLocation>> IMMUNE = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_APPROACH_POS = new HashMap<>();

    private ConditionManager() {
    }

    // ------------------------------------------------------------ apply / remove

    public static boolean apply(ServerPlayer player, ResourceLocation id, int durationTicks, Entity source, int dc) {
        ConditionDefinition definition = id == null ? null : ConditionRegistry.get(id);
        if (definition == null) {
            return false;
        }
        if (isImmune(player, id)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.condition.immune",
                    Component.translatable(definition.nameKey())).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int duration = durationTicks > 0 ? durationTicks : definition.durationTicks();
        int saveDc = dc > 0 ? dc : definition.saveDc();
        int saveTicks = definition.canSave() ? Math.max(1, definition.saveIntervalTicks()) : 0;
        UUID sourceId = source == null ? null : source.getUUID();

        IConditions conditions = resolve(player);
        if (conditions == null) {
            return false;
        }
        boolean existed = conditions.get(id) != null;
        conditions.put(id, new ActiveCondition(duration, saveDc, saveTicks, sourceId));
        applyEffects(player, definition, duration);
        if (!existed && definition.has(ConditionFlag.DROP_ITEMS)) {
            dropHeldItems(player);
        }
        MinecraftForge.EVENT_BUS.post(new DndConditionAppliedEvent(player, id, duration, sourceId));
        player.displayClientMessage(Component.translatable("message.dndclasses.condition.applied",
                Component.translatable(definition.nameKey())).withStyle(ChatFormatting.YELLOW), true);
        ServerStateSync.send(player);
        return true;
    }

    public static boolean remove(ServerPlayer player, ResourceLocation id, DndConditionRemovedEvent.Reason reason) {
        IConditions conditions = resolve(player);
        if (conditions == null || !removeInternal(player, conditions, id, reason)) {
            return false;
        }
        ServerStateSync.send(player);
        return true;
    }

    private static boolean removeInternal(ServerPlayer player, IConditions conditions, ResourceLocation id,
                                          DndConditionRemovedEvent.Reason reason) {
        if (conditions.get(id) == null) {
            return false;
        }
        ConditionDefinition definition = ConditionRegistry.get(id);
        conditions.remove(id);
        if (definition != null) {
            removeEffects(player, definition);
        }
        MinecraftForge.EVENT_BUS.post(new DndConditionRemovedEvent(player, id, reason));
        if (definition != null) {
            player.displayClientMessage(Component.translatable("message.dndclasses.condition.removed",
                    Component.translatable(definition.nameKey())).withStyle(ChatFormatting.GREEN), true);
        }
        return true;
    }

    // ------------------------------------------------------------ tick / saves

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        IConditions conditions = resolve(player);
        if (conditions == null) {
            return;
        }
        boolean changed = false;
        if (!conditions.getActive().isEmpty()) {
            Map<ResourceLocation, DndConditionRemovedEvent.Reason> removals = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, ActiveCondition> entry : new ArrayList<>(conditions.getActive().entrySet())) {
                ResourceLocation id = entry.getKey();
                ActiveCondition active = entry.getValue();
                ConditionDefinition definition = ConditionRegistry.get(id);
                if (definition == null) {
                    removals.put(id, DndConditionRemovedEvent.Reason.OTHER);
                    continue;
                }
                active.durationTicks--;
                if (definition.canSave()) {
                    active.saveTicks--;
                    if (active.saveTicks <= 0) {
                        active.saveTicks = Math.max(1, definition.saveIntervalTicks());
                        int roll = 1 + player.getRandom().nextInt(20);
                        boolean success = roll + Saves.getSaveBonus(player, definition.saveAbility()) >= active.dc;
                        player.displayClientMessage((success
                                ? Component.translatable("message.dndclasses.condition.save_success", Component.translatable(definition.nameKey()))
                                : Component.translatable("message.dndclasses.condition.save_fail", Component.translatable(definition.nameKey())))
                                .withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED), true);
                        if (success) {
                            removals.put(id, DndConditionRemovedEvent.Reason.SAVE);
                            continue;
                        }
                    }
                }
                if (active.durationTicks <= 0) {
                    removals.put(id, DndConditionRemovedEvent.Reason.EXPIRED);
                }
            }
            for (Map.Entry<ResourceLocation, DndConditionRemovedEvent.Reason> entry : removals.entrySet()) {
                if (removeInternal(player, conditions, entry.getKey(), entry.getValue())) {
                    changed = true;
                }
            }
        }
        if (applyApproachClamp(player, conditions)) {
            changed = true;
        }
        if (changed) {
            ServerStateSync.send(player);
        }
    }

    // ------------------------------------------------------------ exhaustion

    public static int getExhaustion(ServerPlayer player) {
        IConditions conditions = resolve(player);
        return conditions == null ? 0 : conditions.getExhaustionLevel();
    }

    public static void setExhaustion(ServerPlayer player, int level) {
        int clamped = Math.max(0, Math.min(6, level));
        IConditions conditions = resolve(player);
        if (conditions != null) {
            conditions.setExhaustionLevel(clamped);
        }
        applyExhaustionModifiers(player, clamped);
        player.displayClientMessage(Component.translatable("message.dndclasses.exhaustion.set", clamped), true);
        ServerStateSync.send(player);
        if (clamped >= 6) {
            player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
        }
    }

    public static void decreaseExhaustion(ServerPlayer player) {
        int level = getExhaustion(player);
        if (level > 0) {
            setExhaustion(player, level - 1);
        }
    }

    private static void applyExhaustionModifiers(ServerPlayer player, int level) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(EXHAUSTION_SPEED);
            double amount = level >= 5 ? -1.0 : (level >= 2 ? -0.5 : 0.0);
            if (amount != 0.0) {
                speed.addTransientModifier(new AttributeModifier(EXHAUSTION_SPEED, "exhaustion speed", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(EXHAUSTION_HEALTH);
            if (level >= 4) {
                health.addTransientModifier(new AttributeModifier(EXHAUSTION_HEALTH, "exhaustion health", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    // ------------------------------------------------------------ queries

    public static boolean has(ServerPlayer player, ResourceLocation id) {
        IConditions conditions = resolve(player);
        return conditions != null && conditions.get(id) != null;
    }

    public static boolean hasFlag(ServerPlayer player, ConditionFlag flag) {
        IConditions conditions = resolve(player);
        if (conditions == null) {
            return false;
        }
        for (ResourceLocation id : conditions.getActive().keySet()) {
            ConditionDefinition definition = ConditionRegistry.get(id);
            if (definition != null && definition.has(flag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIncapacitated(ServerPlayer player) {
        return hasFlag(player, ConditionFlag.INCAPACITATED);
    }

    public static boolean isImmobile(ServerPlayer player) {
        return hasFlag(player, ConditionFlag.IMMOBILE);
    }

    public static boolean halfDamage(ServerPlayer player) {
        return hasFlag(player, ConditionFlag.HALF_DAMAGE);
    }

    public static boolean cannotAttackSource(ServerPlayer player, UUID target) {
        IConditions conditions = resolve(player);
        if (conditions == null || target == null) {
            return false;
        }
        for (Map.Entry<ResourceLocation, ActiveCondition> entry : conditions.getActive().entrySet()) {
            ConditionDefinition definition = ConditionRegistry.get(entry.getKey());
            if (definition != null && definition.has(ConditionFlag.CANNOT_ATTACK_SOURCE)
                    && target.equals(entry.getValue().source)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImmune(ServerPlayer player, ResourceLocation id) {
        Set<ResourceLocation> set = IMMUNE.get(player.getUUID());
        return set != null && set.contains(id);
    }

    public static void setImmune(ServerPlayer player, ResourceLocation id, boolean immune) {
        Set<ResourceLocation> set = IMMUNE.computeIfAbsent(player.getUUID(), key -> new HashSet<>());
        if (immune) {
            set.add(id);
        } else {
            set.remove(id);
        }
    }

    // ------------------------------------------------------------ effects

    private static void applyEffects(ServerPlayer player, ConditionDefinition definition, int duration) {
        removeEffects(player, definition);
        for (ConditionEffect effect : definition.effects()) {
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect.effect());
            if (mobEffect != null) {
                player.addEffect(new MobEffectInstance(mobEffect, duration, effect.amplifier(),
                        effect.ambient(), effect.showParticles(), effect.showIcon()));
            }
        }
        for (int i = 0; i < definition.modifiers().size(); i++) {
            AttributeModifierSpec spec = definition.modifiers().get(i);
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(spec.attribute());
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            UUID modifierId = modifierId(definition.id(), i);
            instance.removeModifier(modifierId);
            instance.addTransientModifier(new AttributeModifier(modifierId, definition.id().toString(), spec.amount(), spec.operation()));
        }
    }

    private static void removeEffects(ServerPlayer player, ConditionDefinition definition) {
        for (ConditionEffect effect : definition.effects()) {
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect.effect());
            if (mobEffect != null) {
                player.removeEffect(mobEffect);
            }
        }
        for (int i = 0; i < definition.modifiers().size(); i++) {
            AttributeModifierSpec spec = definition.modifiers().get(i);
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(spec.attribute());
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifierId(definition.id(), i));
            }
        }
    }

    private static UUID modifierId(ResourceLocation id, int index) {
        return UUID.nameUUIDFromBytes((id + "#" + index).getBytes(StandardCharsets.UTF_8));
    }

    private static void dropHeldItems(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (!player.getItemInHand(hand).isEmpty()) {
                player.drop(player.getItemInHand(hand), false);
                player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    // ------------------------------------------------------------ approach clamp + client sync

    /** Prevents a frightened player from moving closer to the source (server-side clamp). */
    private static boolean applyApproachClamp(ServerPlayer player, IConditions conditions) {
        UUID sourceId = null;
        for (Map.Entry<ResourceLocation, ActiveCondition> entry : conditions.getActive().entrySet()) {
            ConditionDefinition definition = ConditionRegistry.get(entry.getKey());
            if (definition != null && definition.has(ConditionFlag.CANNOT_APPROACH_SOURCE) && entry.getValue().source != null) {
                sourceId = entry.getValue().source;
                break;
            }
        }
        UUID uuid = player.getUUID();
        if (sourceId == null) {
            LAST_APPROACH_POS.remove(uuid);
            return false;
        }
        Entity source = player.serverLevel().getEntity(sourceId);
        if (source == null) {
            LAST_APPROACH_POS.put(uuid, player.position());
            return false;
        }
        Vec3 last = LAST_APPROACH_POS.get(uuid);
        Vec3 current = player.position();
        boolean reverted = false;
        if (last != null) {
            double distance = current.distanceTo(source.position());
            double lastDistance = last.distanceTo(source.position());
            if (distance < lastDistance - 0.02D && current.distanceToSqr(last) < 4.0D) {
                player.connection.teleport(last.x, last.y, last.z, player.getYRot(), player.getXRot());
                current = last;
                reverted = true;
            }
        }
        LAST_APPROACH_POS.put(uuid, current);
        return reverted;
    }

    /** Snapshot for the future HUD (and the client-synced tag). */
    public static CompoundTag buildClientTag(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        IConditions conditions = resolve(player);
        if (conditions == null) {
            return root;
        }
        root.putInt("exhaustion", conditions.getExhaustionLevel());
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, ActiveCondition> entry : conditions.getActive().entrySet()) {
            ConditionDefinition definition = ConditionRegistry.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("id", entry.getKey().toString());
            tag.putLong("flags", flagsMask(definition));
            tag.putInt("duration", entry.getValue().durationTicks);
            if (entry.getValue().source != null) {
                tag.putString("source", entry.getValue().source.toString());
            }
            list.add(tag);
        }
        root.put("conditions", list);
        return root;
    }

    public static long flagsMask(ConditionDefinition definition) {
        long mask = 0L;
        for (ConditionFlag flag : definition.flags()) {
            mask |= 1L << flag.ordinal();
        }
        return mask;
    }

    // ------------------------------------------------------------ lifecycle

    public static void clearOnDeath(ServerPlayer player) {
        IConditions conditions = resolve(player);
        if (conditions != null) {
            conditions.clearNonExhaustion();
        }
        LAST_APPROACH_POS.remove(player.getUUID());
        IMMUNE.remove(player.getUUID());
        ServerStateSync.send(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_APPROACH_POS.remove(event.getEntity().getUUID());
        IMMUNE.remove(event.getEntity().getUUID());
    }

    private static IConditions resolve(ServerPlayer player) {
        return player.getCapability(ModCapabilities.CONDITIONS_CAPABILITY).resolve().orElse(null);
    }

    /** For the future HUD: active conditions with their flags and remaining ticks. */
    public static List<CompoundTag> snapshot(ServerPlayer player) {
        List<CompoundTag> out = new ArrayList<>();
        IConditions conditions = resolve(player);
        if (conditions == null) {
            return out;
        }
        for (Map.Entry<ResourceLocation, ActiveCondition> entry : conditions.getActive().entrySet()) {
            ConditionDefinition definition = ConditionRegistry.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("id", entry.getKey().toString());
            tag.putLong("flags", flagsMask(definition));
            tag.putInt("duration", entry.getValue().durationTicks);
            out.add(tag);
        }
        return out;
    }
}
