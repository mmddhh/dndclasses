package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WeaponEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Dndclasses.MODID);

    public static final RegistryObject<EntityType<ThrownWeaponEntity>> THROWN_WEAPON = ENTITY_TYPES.register("thrown_weapon",
            () -> EntityType.Builder.<ThrownWeaponEntity>of(ThrownWeaponEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("thrown_weapon"));

    private WeaponEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
