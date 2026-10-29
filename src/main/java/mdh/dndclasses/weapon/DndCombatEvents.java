package mdh.dndclasses.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;
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
import mdh.dndclasses.client.ClientReloadState;
import mdh.dndclasses.compat.BetterCombatCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.DistExecutor;

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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            DndCombat.tickReload(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        // Better Combat owns the attack pipeline when it is installed: do not cancel its event,
        // our damage is applied in compat/BetterCombatDamageHandler instead.
        if (BetterCombatCompat.isActive()) {
            return;
        }

        InteractionHand hand = player instanceof ServerPlayer serverPlayer
                ? DualWieldRules.nextHand(serverPlayer)
                : InteractionHand.MAIN_HAND;
        if (DndCooldowns.isCoolingDown(player, hand)) {
            event.setCanceled(true);
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        boolean rangedAsMelee = stack.getItem() instanceof DndWeapon item && item.spec().isRanged();
        if (DndCombat.attack(player, event.getTarget(), hand, rangedAsMelee)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer && DualWieldRules.canDualWield(player)) {
                DualWieldRules.recordAttack(serverPlayer, hand);
            }
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
        // Use actions must run on the server: cancelling the client-side event would prevent the
        // use packet from being sent at all (breaking throws and crossbow shots).
        if (player.level().isClientSide) {
            return false;
        }
        // Right-click is reserved for throwing light weapons from the offhand; melee offhand
        // attacks are performed by the alternating left-click attack instead.
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof DndWeapon offhandWeapon) {
            DndWeaponSpec offhandSpec = offhandWeapon.spec();
            if (offhandSpec.isThrown() && offhandSpec.has(WeaponProperty.LIGHT)) {
                return DndCombat.throwWeapon(player, InteractionHand.OFF_HAND);
            }
        }

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof DndWeapon mainWeapon) {
            DndWeaponSpec spec = mainWeapon.spec();
            if (spec.isRanged()) {
                if (spec.isCrossbow()) {
                    if (main.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED)) {
                        return DndCombat.shootRanged(player, InteractionHand.MAIN_HAND);
                    }
                    // A firing attempt aborts a charge in progress: no progress is kept.
                    DndCombat.cancelReload(player);
                    if (!player.level().isClientSide) {
                        // Also drop the "held in front" pose a cancelled reload left behind.
                        CrossbowItem.setCharged(main, false);
                    }
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientReloadState::clear);
                    player.displayClientMessage(Component.translatable("message.dndclasses.not_loaded"), true);
                    return true;
                }
                if (spec.isBow()) {
                    return false;
                }
                return DndCombat.shootRanged(player, InteractionHand.MAIN_HAND);
            }
            if (spec.isThrown()) {
                return DndCombat.throwWeapon(player, InteractionHand.MAIN_HAND);
            }
        }
        return false;
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
