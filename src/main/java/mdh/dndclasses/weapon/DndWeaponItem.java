package mdh.dndclasses.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.List;
import java.util.UUID;

public class DndWeaponItem extends Item {
    public static final String TAG_LOADED = "DndLoaded";
    public static final String TAG_LOADED_AMMO = "DndLoadedAmmo";
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("424e0a10-700f-48c2-8b4b-cd18e7175a57");
    private static final UUID ENTITY_REACH_MODIFIER = UUID.fromString("b0d0bebc-08f1-4b9f-85e0-566e36b9fb86");

    private final DndWeaponSpec spec;
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public DndWeaponItem(DndWeaponSpec spec, Properties properties) {
        super(properties.stacksTo(1));
        this.spec = spec;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER, "DND weapon attack speed", -2.0D, AttributeModifier.Operation.ADDITION));
        if (spec.has(WeaponProperty.REACH)) {
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(ENTITY_REACH_MODIFIER, "DND reach weapon", 2.0D, AttributeModifier.Operation.ADDITION));
        }
        this.attributeModifiers = builder.build();
    }

    public DndWeaponSpec spec() {
        return spec;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributeModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (spec.has(WeaponProperty.SWORD_DIG) && state.is(Blocks.COBWEB)) {
            return 15.0F;
        }
        if (spec.has(WeaponProperty.AXE_DIG) && (state.is(BlockTags.LOGS) || state.getBlock() instanceof LeavesBlock)) {
            return 6.0F;
        }
        if (spec.has(WeaponProperty.PICKAXE_DIG) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return 4.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (spec.has(WeaponProperty.PICKAXE_DIG) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return true;
        }
        return spec.has(WeaponProperty.AXE_DIG) && state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (spec.has(WeaponProperty.SWORD_DIG) && ToolActions.DEFAULT_SWORD_ACTIONS.contains(toolAction)) {
            return true;
        }
        if (spec.has(WeaponProperty.AXE_DIG) && ToolActions.DEFAULT_AXE_ACTIONS.contains(toolAction)) {
            return true;
        }
        return spec.has(WeaponProperty.PICKAXE_DIG) && ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(toolAction);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dndclasses.damage", spec.oneHandDamage(), spec.damageType().id()).withStyle(ChatFormatting.GRAY));
        if (spec.isVersatile()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.versatile", spec.twoHandDamage()).withStyle(ChatFormatting.GRAY));
        }
        if (spec.magical()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.magical").withStyle(ChatFormatting.AQUA));
        }
        if (!spec.properties().isEmpty()) {
            tooltip.add(Component.literal(spec.properties().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (spec.isLoading()) {
            tooltip.add(Component.translatable("tooltip.dndclasses.loading").withStyle(ChatFormatting.YELLOW));
        }
    }
}
