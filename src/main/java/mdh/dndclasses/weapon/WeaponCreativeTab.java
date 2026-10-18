package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class WeaponCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dndclasses.MODID);

    public static final RegistryObject<CreativeModeTab> DND_WEAPONS = CREATIVE_MODE_TABS.register("dnd_weapons",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(WeaponItems.weapon("iron_longsword").get()))
                    .title(Component.translatable("itemgroup.dndclasses.dnd_weapons"))
                    .displayItems((parameters, output) -> {
                        WeaponItems.WEAPONS.values().forEach(item -> output.accept(item.get()));
                        WeaponItems.AMMO.values().forEach(item -> output.accept(item.get()));
                    }).build());

    private WeaponCreativeTab() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
