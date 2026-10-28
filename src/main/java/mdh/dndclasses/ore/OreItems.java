package mdh.dndclasses.ore;

import mdh.dndclasses.Dndclasses;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OreItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Dndclasses.MODID);

    public static final RegistryObject<Item> ORICHALCUM_INGOT = ITEMS.register("orichalcum_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ORICHALCUM_NUGGET = ITEMS.register("orichalcum_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ORICHALCUM_CHAIN = ITEMS.register("orichalcum_chain", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_ORICHALCUM = ITEMS.register("raw_orichalcum", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MITHRIL_INGOT = ITEMS.register("mithril_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MITHRIL_NUGGET = ITEMS.register("mithril_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MITHRIL_CHAIN = ITEMS.register("mithril_chain", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_MITHRIL = ITEMS.register("raw_mithril", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILVER_INGOT = ITEMS.register("silver_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILVER_NUGGET = ITEMS.register("silver_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_SILVER = ITEMS.register("raw_silver", () -> new Item(new Item.Properties()));

    private OreItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
