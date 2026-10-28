package mdh.dndclasses.gear;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.armor.DndArmorItem;
import mdh.dndclasses.ore.OreItems;
import mdh.dndclasses.weapon.DndWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Anvil repair for D&amp;D gear: one matching ingot repairs 25% (or restores a broken item to 50%). */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DurabilityEvents {

    private DurabilityEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (!DndDurability.isDndGear(left) || left.getDamageValue() <= 0 && !DndDurability.isBroken(left)) {
            return;
        }
        Ingredient repair = repairIngredient(left);
        if (repair == null || !repair.test(event.getRight())) {
            return;
        }

        ItemStack output = left.copy();
        output.setCount(1);
        int max = output.getMaxDamage();
        if (DndDurability.isBroken(output)) {
            DndDurability.setBroken(output, false);
            output.setDamageValue(Math.max(0, max / 2));
        } else {
            output.setDamageValue(Math.max(0, output.getDamageValue() - Math.max(1, max / 4)));
        }
        event.setOutput(output);
        event.setCost(1);
        event.setMaterialCost(1);
    }

    private static Ingredient repairIngredient(ItemStack stack) {
        if (stack.getItem() instanceof DndArmorItem armor) {
            return armor.materialType().repairIngredient();
        }
        if (stack.getItem() instanceof DndWeapon weapon) {
            return switch (weapon.spec().material()) {
                case IRON -> Ingredient.of(Items.IRON_INGOT);
                case ORICHALCUM -> Ingredient.of(OreItems.ORICHALCUM_INGOT.get());
                case SILVER -> Ingredient.of(OreItems.SILVER_INGOT.get());
            };
        }
        return null;
    }
}
