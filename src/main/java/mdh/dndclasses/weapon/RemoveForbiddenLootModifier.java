package mdh.dndclasses.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mdh.dndclasses.Dndclasses;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Strips forbidden vanilla weapons/tools out of generated world loot (chests, mob drops, fishing...). */
public class RemoveForbiddenLootModifier extends LootModifier {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Dndclasses.MODID);

    public static final Codec<RemoveForbiddenLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, RemoveForbiddenLootModifier::new));

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> SERIALIZER =
            SERIALIZERS.register("remove_forbidden", () -> CODEC);

    private RemoveForbiddenLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(stack -> VanillaEquipmentRules.isForbidden(stack.getItem()));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
