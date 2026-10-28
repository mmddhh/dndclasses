package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WeaponItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Dndclasses.MODID);
    public static final Map<String, RegistryObject<? extends Item>> WEAPONS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> AMMO = new LinkedHashMap<>();

    private static final String[] AMMO_SUFFIXES = {"arrow", "bolt", "blowgun_needle", "sling_bullet"};

    static {
        for (DndWeaponSpec spec : DndWeaponCatalog.WEAPONS) {
            if (spec.isCrossbow()) {
                WEAPONS.put(spec.id(), ITEMS.register(spec.id(),
                        () -> new DndCrossbowItem(spec, new Item.Properties().durability(spec.durability()))));
            } else {
                WEAPONS.put(spec.id(), ITEMS.register(spec.id(),
                        () -> new DndWeaponItem(spec, new Item.Properties().durability(spec.durability()))));
            }
        }
        for (DndWeaponMaterial material : DndWeaponMaterial.values()) {
            for (String suffix : AMMO_SUFFIXES) {
                String id = material.id() + "_" + suffix;
                AMMO.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
            }
        }
    }

    private WeaponItems() {
    }

    public static RegistryObject<? extends Item> weapon(String id) {
        return WEAPONS.get(id);
    }

    public static DndWeaponSpec specOf(Item item) {
        return item instanceof DndWeapon weapon ? weapon.spec() : null;
    }

    public static RegistryObject<Item> ammo(String id) {
        return AMMO.get(id);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
