package mdh.dndclasses.ore;

import mdh.dndclasses.Dndclasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class OreCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dndclasses.MODID);

    public static final RegistryObject<CreativeModeTab> DND_MATERIALS = CREATIVE_MODE_TABS.register("dnd_materials",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(OreItems.ORICHALCUM_INGOT.get()))
                    .title(Component.translatable("itemgroup.dndclasses.dnd_materials"))
                    .displayItems((parameters, output) -> {
                        output.accept(OreItems.ORICHALCUM_INGOT.get());
                        output.accept(OreItems.ORICHALCUM_NUGGET.get());
                        output.accept(OreItems.ORICHALCUM_CHAIN.get());
                        output.accept(OreItems.RAW_ORICHALCUM.get());
                        output.accept(OreItems.MITHRIL_INGOT.get());
                        output.accept(OreItems.MITHRIL_NUGGET.get());
                        output.accept(OreItems.MITHRIL_CHAIN.get());
                        output.accept(OreItems.RAW_MITHRIL.get());
                        output.accept(OreItems.SILVER_INGOT.get());
                        output.accept(OreItems.SILVER_NUGGET.get());
                        output.accept(OreItems.RAW_SILVER.get());
                        output.accept(OreBlocks.ORICHALCUM_ORE.get());
                        output.accept(OreBlocks.DEEPSLATE_ORICHALCUM_ORE.get());
                        output.accept(OreBlocks.MITHRIL_ORE.get());
                        output.accept(OreBlocks.DEEPSLATE_MITHRIL_ORE.get());
                        output.accept(OreBlocks.SILVER_ORE.get());
                        output.accept(OreBlocks.DEEPSLATE_SILVER_ORE.get());
                        output.accept(OreBlocks.ORICHALCUM_BLOCK.get());
                        output.accept(OreBlocks.MITHRIL_BLOCK.get());
                        output.accept(OreBlocks.SILVER_BLOCK.get());
                    }).build());

    private OreCreativeTab() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
