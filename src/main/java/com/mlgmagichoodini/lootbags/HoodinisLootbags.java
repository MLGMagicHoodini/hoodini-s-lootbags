package com.mlgmagichoodini.lootbags;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(HoodinisLootbags.MODID)
public class HoodinisLootbags {
    public static final String MODID = "hoodinislootbags";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HoodinisLootbags(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModItems.TABS.register(modEventBus);

        // Config.java still comes from the generator for now; we will replace it in the next step.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
