package com.mlgmagichoodini.lootbags;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class LootBagItem extends Item {
    private static final int MAX_REROLLS = 25;

    private final BagTier tier;

    public LootBagItem(BagTier tier, Item.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public BagTier getTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(tier.getColor());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);

        // Everything below happens on the server only.
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(tier.getLootTableKey());
        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.GIFT);

        // A bag gives exactly ONE item (which can be a stack).
        // If the roll lands on a blocked item (see ItemRules and the config), roll again.
        ItemStack reward = ItemStack.EMPTY;
        for (int attempt = 0; attempt < MAX_REROLLS && reward.isEmpty(); attempt++) {
            for (ItemStack candidate : table.getRandomItems(params)) {
                if (!candidate.isEmpty() && Config.isItemAllowed(candidate)) {
                    reward = candidate;
                    break;
                }
            }
        }

        if (reward.isEmpty()) {
            // Empty table or everything blocked: keep the bag.
            return InteractionResult.FAIL;
        }

        // 1) Remember what we are giving BEFORE touching it (adding to the inventory empties the stack).
        ItemStack given = reward.copy();

        // 2) Give the item first.
        player.getInventory().add(reward);
        if (!reward.isEmpty()) {
            // Inventory was full (or only partly fit): drop whatever is left at the player's feet.
            player.drop(reward, false);
        }

        // 3) Only then use up one bag.
        bag.shrink(1);

        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
        Component message = Component.translatable(
                "message.hoodinislootbags.received", given.getCount(), given.getHoverName());
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(message);
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}