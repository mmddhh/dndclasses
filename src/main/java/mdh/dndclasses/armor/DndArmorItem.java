package mdh.dndclasses.armor;

import mdh.dndclasses.gear.DndDurability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A D&amp;D armor piece: armor type + material. Provides no vanilla armor points. */
public class DndArmorItem extends ArmorItem {

    private final ArmorType armorType;
    private final ArmorMaterialType materialType;

    public DndArmorItem(ArmorType armorType, ArmorMaterialType materialType, ArmorItem.Type slot, Item.Properties properties) {
        super(materialType.material(), slot, properties);
        this.armorType = armorType;
        this.materialType = materialType;
    }

    public ArmorType armorType() {
        return armorType;
    }

    public ArmorMaterialType materialType() {
        return materialType;
    }

    public EquipmentSlot equipmentSlot() {
        return getEquipmentSlot();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dndclasses.armor_ac", armorType.baseAc(),
                Component.translatable("tooltip.dndclasses.armor_category." + armorType.category().name().toLowerCase()))
                .withStyle(ChatFormatting.GRAY));
        if (armorType.strengthRequirement() > 0) {
            tooltip.add(Component.translatable("tooltip.dndclasses.armor_strength", armorType.strengthRequirement())
                    .withStyle(materialType.mithril() ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY));
        }
        if (armorType.stealthDisadvantage() && !materialType.mithril()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.armor_stealth").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (materialType.critImmune()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.armor_orichalcum").withStyle(ChatFormatting.AQUA));
        }
        if (materialType.mithril()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.armor_mithril").withStyle(ChatFormatting.AQUA));
        }
        if (DndDurability.isBroken(stack)) {
            tooltip.add(Component.translatable("tooltip.dndclasses.broken").withStyle(ChatFormatting.RED));
        }
    }
}
