package mdh.dndclasses;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_EXPERIENCE_OVERRIDES = BUILDER.comment("Custom DND experience for killed entities. Format: namespace:path=xp. Missing hostile mobs use the default health formula; passive mobs give 0 by default.").defineListAllowEmpty("entityExperienceOverrides", List.of("minecraft:ender_dragon=25000", "minecraft:wither=25000"), Config::validateEntityExperienceOverride);

    private static final ForgeConfigSpec.IntValue DEFAULT_HOSTILE_XP_PER_HEALTH = BUILDER.comment("DND experience multiplier per max health point for hostile mobs not listed in entityExperienceOverrides.").defineInRange("defaultHostileXpPerHealth", 5, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DEFAULT_HOSTILE_MIN_XP = BUILDER.comment("Minimum DND experience for hostile mobs not listed in entityExperienceOverrides.").defineInRange("defaultHostileMinXp", 10, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items = new HashSet<>();
    public static Map<ResourceLocation, Integer> entityExperienceOverrides = Map.of(
            new ResourceLocation("minecraft", "ender_dragon"), 25000,
            new ResourceLocation("minecraft", "wither"), 25000
    );
    public static int defaultHostileXpPerHealth = 5;
    public static int defaultHostileMinXp = 10;

    private static boolean validateItemName(final Object obj) {
        try {
            return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean validateEntityExperienceOverride(final Object obj) {
        if (!(obj instanceof final String entry)) {
            return false;
        }

        String[] parts = entry.split("=", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            new ResourceLocation(parts[0]);
            return Integer.parseInt(parts[1]) >= 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream().map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName))).collect(Collectors.toSet());

        Map<ResourceLocation, Integer> overrides = new HashMap<>();
        for (String entry : ENTITY_EXPERIENCE_OVERRIDES.get()) {
            String[] parts = entry.split("=", 2);
            try {
                overrides.put(new ResourceLocation(parts[0]), Integer.parseInt(parts[1]));
            } catch (RuntimeException ignored) {
            }
        }
        entityExperienceOverrides = Map.copyOf(overrides);
        defaultHostileXpPerHealth = DEFAULT_HOSTILE_XP_PER_HEALTH.get();
        defaultHostileMinXp = DEFAULT_HOSTILE_MIN_XP.get();
    }
}
