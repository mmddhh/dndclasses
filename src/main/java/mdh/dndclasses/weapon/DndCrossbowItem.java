package mdh.dndclasses.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import mdh.dndclasses.gear.DndDurability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.List;
import java.util.UUID;

/**
 * Crossbow registered as a real {@link CrossbowItem} so vanilla first/third-person charge and hold
 * rendering works. Loading ammo and damage remain D&amp;D-specific.
 */
public class DndCrossbowItem extends CrossbowItem implements DndWeapon {

    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("424e0a10-700f-48c2-8b4b-cd18e7175a57");
    private static final UUID ENTITY_REACH_MODIFIER = UUID.fromString("b0d0bebc-08f1-4b9f-85e0-566e36b9fb86");

    private final DndWeaponSpec spec;
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public DndCrossbowItem(DndWeaponSpec spec, Item.Properties properties) {
        super(properties);
        this.spec = spec;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER,
                "DND weapon attack speed", -2.0D, AttributeModifier.Operation.ADDITION));
        if (spec.has(WeaponProperty.REACH)) {
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(ENTITY_REACH_MODIFIER,
                    "DND reach weapon", 2.0D, AttributeModifier.Operation.ADDITION));
        }
        this.attributeModifiers = builder.build();
    }

    @Override
    public DndWeaponSpec spec() {
        return spec;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributeModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Firing and reloading are both driven by DndCombat; never start a vanilla charge here,
        // otherwise right clicking a block would reload the crossbow outside the R key flow.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // Same charge length as vanilla, so the charge animation and the server timer agree.
        return CrossbowItem.getChargeDuration(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return false;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // Intentionally empty: releasing the charge early must not finish the reload. Loading is
        // resolved by DndCombat's timed charge (which also cancels when the animation stops) and by
        // finishUsingItem at the natural end of the animation.
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            DndCombat.completeLoading(player, player.getUsedItemHand());
        }
        return stack;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (spec.has(WeaponProperty.AXE_DIG) && (state.is(BlockTags.LOGS) || state.getBlock() instanceof LeavesBlock)) {
            return 6.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return spec.has(WeaponProperty.AXE_DIG) && state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (spec.has(WeaponProperty.AXE_DIG) && ToolActions.DEFAULT_AXE_ACTIONS.contains(toolAction)) {
            return true;
        }
        return super.canPerformAction(stack, toolAction);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        WeaponTooltips.append(stack, level, spec, tooltip, flag);
    }
}
