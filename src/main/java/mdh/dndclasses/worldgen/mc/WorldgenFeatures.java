package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.Dndclasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** Registers the data-driven planned-ruin feature. */
public final class WorldgenFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Dndclasses.MODID);

    private WorldgenFeatures() {
    }

    public static void register(IEventBus bus) {
        FEATURES.register("planned_ruin", PlannedRuinFeature::new);
        FEATURES.register(bus);
    }
}
