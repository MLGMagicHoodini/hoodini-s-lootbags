package com.mlgmagichoodini.lootbags;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = HoodinisLootbags.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PLAYER_KILLS_ONLY;
    public static final ModConfigSpec.BooleanValue ONE_BAG_PER_KILL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_MULTIPLIERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_WHITELIST;
    public static final ModConfigSpec.BooleanValue ALLOW_SPAWN_EGGS;

    public static final Map<BagTier, ModConfigSpec.BooleanValue> BAG_ENABLED = new EnumMap<>(BagTier.class);
    public static final Map<BagTier, ModConfigSpec.DoubleValue> DROP_CHANCE = new EnumMap<>(BagTier.class);

    static {
        BUILDER.translation(key("drops")).push("drops");
        PLAYER_KILLS_ONLY = BUILDER
                .comment("Bags only drop when a player (or something a player is responsible for) kills the mob.")
                .translation(key("drops.playerKillsOnly")).define("playerKillsOnly", true);
        ONE_BAG_PER_KILL = BUILDER
                .comment("If true, a single kill can drop at most ONE bag (rolled from the rarest tier down).",
                        "If false, every tier rolls on its own and a kill could drop several bags.")
                .translation(key("drops.oneBagPerKill")).define("oneBagPerKill", true);
        MOB_BLACKLIST = BUILDER
                .comment("Mobs that never drop bags. Use entity IDs, e.g. \"minecraft:slime\".")
                .translation(key("drops.mobBlacklist")).defineListAllowEmpty("mobBlacklist",
                        List.of("minecraft:slime", "minecraft:magma_cube"),
                        () -> "", Config::validateId);
        MOB_MULTIPLIERS = BUILDER
                .comment("Drop chance multipliers for specific mobs, written as \"entity_id=multiplier\".",
                        "Example: \"minecraft:wither_skeleton=2.0\" doubles every tier's chance for that mob.")
                .translation(key("drops.mobMultipliers")).defineListAllowEmpty("mobMultipliers",
                        List.of("minecraft:wither_skeleton=2.0", "minecraft:enderman=1.5", "minecraft:blaze=1.5",
                                "minecraft:elder_guardian=10.0", "minecraft:warden=10.0",
                                "minecraft:wither=25.0", "minecraft:ender_dragon=25.0"),
                        () -> "", Config::validateMultiplier);
        BUILDER.pop();

        BUILDER.translation(key("contents")).push("contents");
        ALLOW_SPAWN_EGGS = BUILDER
                .comment("Can spawn eggs come out of bags? (They are blocked by default.)")
                .translation(key("contents.allowSpawnEggs")).define("allowSpawnEggs", false);
        ITEM_BLACKLIST = BUILDER
                .comment("Extra items that can never come out of a bag. Use item IDs, e.g. \"minecraft:elytra\".",
                        "Some items (bedrock, command blocks, spawners...) are ALWAYS blocked and cannot be unblocked.",
                        "Bag contents live in data/hoodinislootbags/loot_table/bags/<tier>.json, which datapacks can override.")
                .translation(key("contents.itemBlacklist")).defineListAllowEmpty("itemBlacklist", List.of(), () -> "", Config::validateId);
        ITEM_WHITELIST = BUILDER
                .comment("If this list is NOT empty, ONLY these items can come out of bags (everything else is skipped).",
                        "Leave it empty to allow everything that is not blocked.")
                .translation(key("contents.itemWhitelist")).defineListAllowEmpty("itemWhitelist", List.of(), () -> "", Config::validateId);
        BUILDER.pop();

        BUILDER.translation(key("bags")).push("bags");
        double[] defaultChances = {0.08, 0.04, 0.015, 0.005, 0.0015, 0.0003};
        for (BagTier tier : BagTier.values()) {
            BUILDER.translation(key("bags." + tier.getId())).push(tier.getId());
            BAG_ENABLED.put(tier, BUILDER
                    .comment("Set to false to stop this bag from dropping from mobs.")
                    .translation(key("bags." + tier.getId() + ".enabled")).define("enabled", true));
            DROP_CHANCE.put(tier, BUILDER
                    .comment("Chance (0.0 to 1.0) for this bag to drop from a hostile mob kill. 0.01 = 1%.")
                    .translation(key("bags." + tier.getId() + ".dropChance")).defineInRange("dropChance", defaultChances[tier.ordinal()], 0.0, 1.0));
            BUILDER.pop();
        }
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Parsed copies of the lists above, rebuilt every time the config loads or reloads.
    private static volatile Set<Identifier> mobBlacklist = Set.of();
    private static volatile Set<Identifier> itemBlacklist = Set.of();
    private static volatile Set<Identifier> itemWhitelist = Set.of();
    private static volatile Map<Identifier, Double> mobMultipliers = Map.of();

    /** Translation keys for the in-game config screen (texts are in assets/hoodinislootbags/lang/en_us.json). */
    private static String key(String path) {
        return HoodinisLootbags.MODID + ".configuration." + path;
    }

    private static boolean validateId(Object obj) {
        return obj instanceof String s && Identifier.tryParse(s.trim()) != null;
    }

    private static boolean validateMultiplier(Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        int eq = s.indexOf('=');
        if (eq < 1) {
            return false;
        }
        try {
            Double.parseDouble(s.substring(eq + 1).trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return Identifier.tryParse(s.substring(0, eq).trim()) != null;
    }

    private static Set<Identifier> parseIds(List<? extends String> list) {
        Set<Identifier> result = new HashSet<>();
        for (String entry : list) {
            Identifier id = Identifier.tryParse(entry.trim());
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        mobBlacklist = parseIds(MOB_BLACKLIST.get());
        itemBlacklist = parseIds(ITEM_BLACKLIST.get());
        itemWhitelist = parseIds(ITEM_WHITELIST.get());

        Map<Identifier, Double> multipliers = new HashMap<>();
        for (String entry : MOB_MULTIPLIERS.get()) {
            int eq = entry.indexOf('=');
            if (eq < 1) {
                continue;
            }
            Identifier id = Identifier.tryParse(entry.substring(0, eq).trim());
            try {
                double value = Double.parseDouble(entry.substring(eq + 1).trim());
                if (id != null) {
                    multipliers.put(id, value);
                }
            } catch (NumberFormatException ignored) {
                // validateMultiplier already rejects these; nothing to do.
            }
        }
        mobMultipliers = multipliers;
    }

    public static boolean isMobBlacklisted(Identifier mobId) {
        return mobBlacklist.contains(mobId);
    }

    public static double getMobMultiplier(Identifier mobId) {
        return mobMultipliers.getOrDefault(mobId, 1.0);
    }

    /** Decides whether an item is allowed to come out of a bag. */
    public static boolean isItemAllowed(ItemStack stack) {
        Item item = stack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);

        if (ItemRules.ALWAYS_BLOCKED.contains(id)) {
            return false;
        }
        if (item instanceof SpawnEggItem && !ALLOW_SPAWN_EGGS.get()) {
            return false;
        }
        if (itemBlacklist.contains(id)) {
            return false;
        }
        Set<Identifier> whitelist = itemWhitelist;
        return whitelist.isEmpty() || whitelist.contains(id);
    }
}
