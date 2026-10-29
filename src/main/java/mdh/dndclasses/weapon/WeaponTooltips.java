package mdh.dndclasses.weapon;

import mdh.dndclasses.gear.DndDurability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** Shared tooltip for every D&amp;D weapon (melee, thrown, ranged, crossbows). */
public final class WeaponTooltips {

    private WeaponTooltips() {
    }

    public static void append(ItemStack stack, Level level, DndWeaponSpec spec, List<Component> tooltip,
                              TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dndclasses.damage",
                        spec.oneHandDamage(),
                        Component.translatable("tooltip.dndclasses.damage_type." + spec.damageType().id()))
                .withStyle(ChatFormatting.GRAY));
        if (spec.isVersatile()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.versatile", spec.twoHandDamage())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (spec.magical()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.magical").withStyle(ChatFormatting.AQUA));
        }
        spec.properties().stream()
                .filter(property -> property != WeaponProperty.LOADING && property != WeaponProperty.VERSATILE)
                .sorted()
                .forEach(property -> tooltip.add(Component.translatable(
                                "tooltip.dndclasses.property." + property.name().toLowerCase(Locale.ROOT))
                        .withStyle(ChatFormatting.DARK_GRAY)));
        if (spec.isLoading()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.loading").withStyle(ChatFormatting.YELLOW));
        }
        if (DndDurability.isBroken(stack)) {
            tooltip.add(Component.translatable("tooltip.dndclasses.broken").withStyle(ChatFormatting.RED));
        }
    }
}
