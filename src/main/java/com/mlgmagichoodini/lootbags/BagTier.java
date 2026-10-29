package com.mlgmagichoodini.lootbags;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;

public enum BagTier {
    COMMON("common", ChatFormatting.WHITE),
    UNCOMMON("uncommon", ChatFormatting.GREEN),
    RARE("rare", ChatFormatting.AQUA),
    EPIC("epic", ChatFormatting.LIGHT_PURPLE),
    LEGENDARY("legendary", ChatFormatting.GOLD),
    MYTHIC("mythic", ChatFormatting.RED);

    private final String id;
    private final ChatFormatting color;

    BagTier(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public ChatFormatting getColor() {
        return color;
    }

    /** The loot table that decides what is inside this bag: data/hoodinislootbags/loot_table/bags/<id>.json */
    public ResourceKey<LootTable> getLootTableKey() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath(HoodinisLootbags.MODID, "bags/" + id));
    }
}
