package mdh.dndclasses;

import com.mojang.logging.LogUtils;
import mdh.dndclasses.armor.ArmorCreativeTab;
import mdh.dndclasses.armor.ArmorItems;
import mdh.dndclasses.feature.EffectRegistry;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.ore.OreBlocks;
import mdh.dndclasses.ore.OreCreativeTab;
import mdh.dndclasses.ore.OreItems;
import mdh.dndclasses.weapon.RemoveForbiddenLootModifier;
import mdh.dndclasses.weapon.WeaponCreativeTab;
import mdh.dndclasses.weapon.WeaponEntities;
import mdh.dndclasses.weapon.WeaponItems;
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

        WeaponItems.register(modEventBus);
        WeaponEntities.register(modEventBus);
        WeaponCreativeTab.register(modEventBus);
        OreBlocks.register(modEventBus);
        OreItems.register(modEventBus);
        OreCreativeTab.register(modEventBus);
        ArmorItems.register(modEventBus);
        ArmorCreativeTab.register(modEventBus);
        RemoveForbiddenLootModifier.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
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
