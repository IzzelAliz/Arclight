package io.izzel.arclight.common.bridge.core.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface EnchantmentMenuBridge {

    static void bridge$fillEnchantmentOffers(net.minecraft.util.RandomSource random, int seed, int bookcases,
                                             ItemStack itemStack, net.minecraft.core.RegistryAccess registryAccess,
                                             int[] costs, int[] enchantClue, int[] levelClue) {
        random.setSeed(seed);
        for (int slot = 0; slot < costs.length; slot++) {
            costs[slot] = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentCost(random, slot, bookcases, itemStack);
            enchantClue[slot] = -1;
            levelClue[slot] = -1;
            if (costs[slot] < slot + 1) {
                costs[slot] = 0;
            }
        }
        var holders = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).asHolderIdMap();
        for (int slot = 0; slot < costs.length; slot++) {
            if (costs[slot] > 0) {
                var list = arclight$getEnchantmentList(registryAccess, itemStack, slot, costs[slot], random, seed);
                if (!list.isEmpty()) {
                    var enchantment = list.get(random.nextInt(list.size()));
                    enchantClue[slot] = holders.getId(enchantment.enchantment());
                    levelClue[slot] = enchantment.level();
                }
            }
        }
    }

    private static java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> arclight$getEnchantmentList(
            net.minecraft.core.RegistryAccess registryAccess, ItemStack itemStack, int slot, int cost,
            net.minecraft.util.RandomSource random, int seed) {
        random.setSeed(seed + slot);
        var tag = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .get(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) {
            return java.util.List.of();
        }
        var list = net.minecraft.world.item.enchantment.EnchantmentHelper.selectEnchantment(random, itemStack, cost, tag.get().stream());
        if (itemStack.is(net.minecraft.world.item.Items.BOOK) && list.size() > 1) {
            list.remove(random.nextInt(list.size()));
        }
        return list;
    }

    default float bridge$forge$getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return state.is(BlockTags.ENCHANTMENT_POWER_PROVIDER) ? 1 : 0;
    }

    default int bridge$forge$onEnchantmentLevelSet(Level level, BlockPos pos, int enchantRow, int power, ItemStack itemStack, int enchantmentLevel) {
        return enchantmentLevel;
    }
}
