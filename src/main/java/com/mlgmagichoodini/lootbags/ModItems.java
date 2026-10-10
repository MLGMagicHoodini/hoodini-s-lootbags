package com.mlgmagichoodini.lootbags;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HoodinisLootbags.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HoodinisLootbags.MODID);

    /** One bag item per tier, e.g. hoodinislootbags:common_lootbag */
    public static final Map<BagTier, DeferredItem<LootBagItem>> BAGS = new EnumMap<>(BagTier.class);

    static {
        for (BagTier tier : BagTier.values()) {
            BAGS.put(tier, ITEMS.registerItem(tier.getId() + "_lootbag",
                    properties -> new LootBagItem(tier, properties)));
        }
    }

    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + HoodinisLootbags.MODID))
            .icon(() -> new ItemStack(BAGS.get(BagTier.LEGENDARY).get()))
            .displayItems((parameters, output) -> BAGS.values().forEach(bag -> output.accept(bag.get())))
            .build());
}
