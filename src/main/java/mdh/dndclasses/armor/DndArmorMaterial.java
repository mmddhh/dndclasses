package mdh.dndclasses.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;

/** Custom armor material. Defense is always 0: this mod uses its own AC system. */
public final class DndArmorMaterial implements ArmorMaterial {

    private static final Map<ArmorMaterialType, DndArmorMaterial> CACHE = new EnumMap<>(ArmorMaterialType.class);

    private final String name;
    private final int durability;
    private final ArmorMaterialType type;

    private DndArmorMaterial(ArmorMaterialType type) {
        this.type = type;
        this.name = "dndclasses:" + type.id();
        this.durability = type.durability();
    }

    public static DndArmorMaterial of(ArmorMaterialType type) {
        return CACHE.computeIfAbsent(type, DndArmorMaterial::new);
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type slot) {
        return durability;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type slot) {
        return 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 9;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return type.repairIngredient();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
