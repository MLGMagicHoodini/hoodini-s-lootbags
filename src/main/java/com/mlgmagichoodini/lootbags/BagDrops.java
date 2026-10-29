package com.mlgmagichoodini.lootbags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Adds lootbags to the drops of hostile mobs. */
@EventBusSubscriber(modid = HoodinisLootbags.MODID)
public class BagDrops {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        // "Hostile" = anything vanilla marks as an Enemy (zombies, skeletons, creepers, blazes, wither, dragon...)
        if (!(victim instanceof Enemy)) {
            return;
        }
        if (Config.PLAYER_KILLS_ONLY.get() && !(event.getSource().getEntity() instanceof Player)) {
            return;
        }

        Identifier mobId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (Config.isMobBlacklisted(mobId)) {
            return;
        }
        double multiplier = Config.getMobMultiplier(mobId);

        RandomSource random = victim.getRandom();
        boolean onlyOne = Config.ONE_BAG_PER_KILL.get();

        // Roll from the rarest tier down, so rare bags get their chance first.
        BagTier[] tiers = BagTier.values();
        for (int i = tiers.length - 1; i >= 0; i--) {
            BagTier tier = tiers[i];
            if (!Config.BAG_ENABLED.get(tier).get()) {
                continue;
            }
            double chance = Math.min(1.0, Config.DROP_CHANCE.get(tier).get() * multiplier);
            if (random.nextDouble() < chance) {
                ItemStack bag = new ItemStack(ModItems.BAGS.get(tier).get());
                event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), bag));
                if (onlyOne) {
                    return;
                }
            }
        }
    }
}
