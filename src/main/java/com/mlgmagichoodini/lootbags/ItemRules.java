package com.mlgmagichoodini.lootbags;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;

/**
 * Items that can NEVER come out of a lootbag, no matter what the config or a datapack says.
 * (Spawn eggs are handled separately by a config option.)
 */
public class ItemRules {
    public static final Set<ResourceLocation> ALWAYS_BLOCKED = Stream.of(
            "air",
            "bedrock",
            "barrier",
            "light",
            "command_block",
            "chain_command_block",
            "repeating_command_block",
            "command_block_minecart",
            "structure_block",
            "structure_void",
            "jigsaw",
            "debug_stick",
            "knowledge_book",
            "spawner",
            "trial_spawner",
            "vault",
            "end_portal_frame",
            "reinforced_deepslate",
            "petrified_oak_slab",
            "budding_amethyst"
    ).map(ResourceLocation::withDefaultNamespace).collect(Collectors.toUnmodifiableSet());

    private ItemRules() {
    }
}
