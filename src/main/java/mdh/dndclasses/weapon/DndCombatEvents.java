package mdh.dndclasses.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import mdh.dndclasses.Dndclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Weapon combat rules. Vanilla melee weapons/tools are made <em>inert</em> (weak improvised attacks,
 * blocked right-click) rather than deleted; crafting is disabled by data-pack recipe overrides and a
 * global loot modifier strips them from world loot. Only trades are filtered here.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID)
public final class DndCombatEvents {

    private DndCombatEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        boolean rangedAsMelee = main.getItem() instanceof DndWeaponItem item && item.spec().isRanged();
        if (DndCombat.attack(player, event.getTarget(), InteractionHand.MAIN_HAND, rangedAsMelee)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (handleRightClick(player, event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (handleRightClick(player, null)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (VanillaEquipmentRules.isForbidden(event.getItemStack().getItem())) {
            player.displayClientMessage(Component.translatable("message.dndclasses.disabled_item"), true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (VanillaEquipmentRules.isForbidden(event.getItemStack().getItem())) {
            event.setUseItem(Event.Result.DENY);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        BlockState state = event.getState();
        if (stack.is(Items.IRON_PICKAXE) && state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), event.getOriginalSpeed() * 1.5F));
        }
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.getEntity().getMainHandItem().is(Items.IRON_PICKAXE) && event.getTargetBlock().is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(DndCombatEvents::filterTradeList);
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        filterTradeList(event.getGenericTrades());
        filterTradeList(event.getRareTrades());
    }

    private static boolean handleRightClick(Player player, Entity target) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof DndWeaponItem offhandWeapon) {
            if (offhandWeapon.spec().isThrown()) {
                return DndCombat.throwWeapon(player, InteractionHand.OFF_HAND);
            }
            if (target == null) {
                target = findEntityInReach(player, offhandWeapon.spec().reach());
            }
            if (target != null) {
                return DndCombat.attack(player, target, InteractionHand.OFF_HAND, false);
            }
        }

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof DndWeaponItem mainWeapon) {
            DndWeaponSpec spec = mainWeapon.spec();
            if (spec.isRanged()) {
                return DndCombat.shootRanged(player, InteractionHand.MAIN_HAND);
            }
            if (spec.isThrown()) {
                return DndCombat.throwWeapon(player, InteractionHand.MAIN_HAND);
            }
        }
        return false;
    }

    private static Entity findEntityInReach(Player player, double reach) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(reach));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end, searchBox,
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator());
        return hit == null ? null : hit.getEntity();
    }

    private static void filterTradeList(List<VillagerTrades.ItemListing> trades) {
        trades.removeIf(listing -> {
            try {
                MerchantOffer offer = listing.getOffer(null, RandomSource.create());
                return offer != null && isForbiddenOffer(offer);
            } catch (RuntimeException ignored) {
                return false;
            }
        });
    }

    private static boolean isForbiddenOffer(MerchantOffer offer) {
        return VanillaEquipmentRules.isForbidden(offer.getResult().getItem())
                || VanillaEquipmentRules.isForbidden(offer.getBaseCostA().getItem())
                || VanillaEquipmentRules.isForbidden(offer.getCostB().getItem());
    }

    @Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            List<ItemStack> remove = new ArrayList<>();
            for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : event.getEntries()) {
                Item item = entry.getKey().getItem();
                if (VanillaEquipmentRules.isForbidden(item)) {
                    remove.add(entry.getKey());
                }
            }
            remove.forEach(stack -> event.getEntries().remove(stack));
        }
    }
}
