package mdh.dndclasses.feature;

import mdh.dndclasses.capability.ModCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Applies the passive features a player currently qualifies for, keeps their active-feature
 * resource pools in sync, and removes what they lost. Uses a per-player "applied" snapshot so
 * refresh is a cheap idempotent diff.
 */
public final class FeatureManager {

    public enum UseResult { SUCCESS, UNKNOWN_FEATURE, NOT_ACTIVE, NO_USES }

    private static final Map<UUID, Map<ResourceLocation, FeatureDefinition>> APPLIED = new HashMap<>();

    private FeatureManager() {
    }

    public static void clearApplied(UUID uuid) {
        APPLIED.remove(uuid);
    }

    public static void refreshAll(ServerPlayer player) {
        Map<ResourceLocation, FeatureDefinition> active = activeFeatures(player);
        Map<ResourceLocation, FeatureDefinition> previous =
                APPLIED.getOrDefault(player.getUUID(), Map.of());

        for (Map.Entry<ResourceLocation, FeatureDefinition> entry : previous.entrySet()) {
            FeatureDefinition now = active.get(entry.getKey());
            if (now == null || !now.equals(entry.getValue())) {
                removeEffects(player, entry.getValue());
            }
        }
        for (Map.Entry<ResourceLocation, FeatureDefinition> entry : active.entrySet()) {
            FeatureDefinition before = previous.get(entry.getKey());
            if (before == null || !before.equals(entry.getValue())) {
                applyEffects(player, entry.getValue());
            }
        }

        APPLIED.put(player.getUUID(), active);
    }

    public static Map<ResourceLocation, FeatureDefinition> activeFeatures(ServerPlayer player) {
        Map<ResourceLocation, FeatureDefinition> result = new LinkedHashMap<>();
        player.getCapability(ModCapabilities.LEVEL_CAPABILITY).ifPresent(level -> {
            if (!level.hasCreatedCharacter()) {
                return;
            }
            ResourceLocation classId = id("dndclasses", level.getclassName());
            if (classId == null) {
                return;
            }
            int playerLevel = level.getlevel();
            collect(result, classId, playerLevel);

            String subclassRaw = level.getSubclassKey();
            if (subclassRaw != null && !subclassRaw.isEmpty()) {
                ResourceLocation subclassId = ResourceLocation.tryParse(subclassRaw);
                if (subclassId != null) {
                    collect(result, subclassId, playerLevel);
                }
            }
        });
        return result;
    }

    public static List<FeatureDefinition> activeFeatureList(ServerPlayer player) {
        return new ArrayList<>(activeFeatures(player).values());
    }

    /** Trigger an active feature: check qualification + uses, run use-effects, spend one use. */
    public static UseResult use(ServerPlayer player, ResourceLocation featureId) {
        FeatureDefinition feature = activeFeatures(player).get(featureId);
        if (feature == null) {
            return UseResult.UNKNOWN_FEATURE;
        }
        if (feature.kind() != FeatureDefinition.Kind.ACTIVE) {
            return UseResult.NOT_ACTIVE;
        }

        boolean[] success = {false};
        boolean[] noUses = {false};
        player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(skill -> {
            String key = featureId.toString();
            int uses = skill.getskilluse(key);
            if (uses <= 0) {
                noUses[0] = true;
                return;
            }
            for (EffectDefinition effect : feature.effects()) {
                EffectHandler handler = EffectRegistry.get(effect.type());
                if (handler != null) {
                    handler.use(player, feature, effect);
                }
            }
            skill.setskilluse(key, uses - 1);
            success[0] = true;
        });

        return success[0] ? UseResult.SUCCESS : UseResult.NO_USES;
    }

    public static int remainingUses(ServerPlayer player, ResourceLocation featureId) {
        int[] value = {0};
        player.getCapability(ModCapabilities.SKILL_CAPABILITY)
                .ifPresent(skill -> value[0] = skill.getskilluse(featureId.toString()));
        return value[0];
    }

    private static void collect(Map<ResourceLocation, FeatureDefinition> result, ResourceLocation owner, int playerLevel) {
        for (FeatureDefinition definition : FeatureRegistry.byOwner(owner)) {
            if (definition.level() <= playerLevel) {
                result.put(definition.id(), definition);
            }
        }
    }

    private static void applyEffects(ServerPlayer player, FeatureDefinition feature) {
        if (feature.kind() == FeatureDefinition.Kind.ACTIVE) {
            ensureResource(player, feature);
            return;
        }
        for (EffectDefinition effect : feature.effects()) {
            EffectHandler handler = EffectRegistry.get(effect.type());
            if (handler != null) {
                handler.apply(player, feature, effect);
            }
        }
    }

    private static void removeEffects(ServerPlayer player, FeatureDefinition feature) {
        if (feature.kind() == FeatureDefinition.Kind.ACTIVE) {
            removeResource(player, feature);
            return;
        }
        for (EffectDefinition effect : feature.effects()) {
            EffectHandler handler = EffectRegistry.get(effect.type());
            if (handler != null) {
                handler.remove(player, feature, effect);
            }
        }
    }

    /** Active features keep a use-counter in the skill capability (short/long rest recovery is built in). */
    private static void ensureResource(ServerPlayer player, FeatureDefinition feature) {
        player.getCapability(ModCapabilities.SKILL_CAPABILITY).ifPresent(skill -> {
            String key = feature.id().toString();
            int max = Math.max(1, feature.maxUses());
            boolean shortRest = feature.rest() == FeatureDefinition.Rest.SHORT;
            if (skill.getmaxskilluse(key) <= 0) {
                // addskill creates the pool at full; only call it when missing so we never reset uses
                skill.addskill(key, max, shortRest);
            } else {
                skill.setmaxskilluse(key, max);
            }
        });
    }

    private static void removeResource(ServerPlayer player, FeatureDefinition feature) {
        player.getCapability(ModCapabilities.SKILL_CAPABILITY)
                .ifPresent(skill -> skill.removeSkill(feature.id().toString()));
    }

    private static ResourceLocation id(String namespace, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryParse(namespace + ":" + path);
    }
}
