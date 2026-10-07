package mdh.dndclasses;

import com.mojang.logging.LogUtils;
import mdh.dndclasses.feature.EffectRegistry;
import mdh.dndclasses.network.DndNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Dndclasses.MODID)
public class Dndclasses {

    public static final String MODID = "dndclasses";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Dndclasses() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        DndNetwork.register();
        EffectRegistry.registerDefaults();
        LOGGER.info("dndClasses mod loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("dndClasses server starting");
    }
}
