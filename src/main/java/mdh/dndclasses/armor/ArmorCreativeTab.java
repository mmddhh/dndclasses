package mdh.dndclasses.armor;

import mdh.dndclasses.Dndclasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ArmorCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dndclasses.MODID);

    public static final RegistryObject<CreativeModeTab> DND_ARMOR = CREATIVE_MODE_TABS.register("dnd_armor",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ArmorItems.ARMOR.get("iron_plate_chestplate").get()))
                    .title(Component.translatable("itemgroup.dndclasses.dnd_armor"))
                    .displayItems((parameters, output) -> ArmorItems.ARMOR.values().forEach(item -> output.accept(item.get())))
                    .build());

    private ArmorCreativeTab() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
