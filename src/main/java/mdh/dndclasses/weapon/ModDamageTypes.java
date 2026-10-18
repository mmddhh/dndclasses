package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Map;

public final class ModDamageTypes {
    private static final Map<DndDamageType, ResourceKey<DamageType>> KEYS = new EnumMap<>(DndDamageType.class);

    static {
        for (DndDamageType type : DndDamageType.values()) {
            KEYS.put(type, ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Dndclasses.MODID, type.id())));
        }
    }

    private ModDamageTypes() {
    }

    public static ResourceKey<DamageType> key(DndDamageType type) {
        return KEYS.get(type);
    }

    public static DamageSource source(Level level, DndDamageType type, Entity direct, Entity attacker) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder.Reference<DamageType> holder = registry.getHolderOrThrow(key(type));
        return new DamageSource(holder, direct, attacker);
    }
}
