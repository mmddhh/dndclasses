package mdh.dndclasses.armor;

import mdh.dndclasses.Dndclasses;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ArmorItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Dndclasses.MODID);
    public static final Map<String, RegistryObject<DndArmorItem>> ARMOR = new LinkedHashMap<>();

    static {
        for (ArmorMaterialType material : ArmorMaterialType.values()) {
            for (ArmorType type : ArmorType.values()) {
                for (ArmorItem.Type slot : ArmorItem.Type.values()) {
                    String id = material.id() + "_" + type.id() + "_" + slot.getName();
                    ARMOR.put(id, ITEMS.register(id,
                            () -> new DndArmorItem(type, material, slot, new Item.Properties())));
                }
            }
        }
    }

    private ArmorItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
