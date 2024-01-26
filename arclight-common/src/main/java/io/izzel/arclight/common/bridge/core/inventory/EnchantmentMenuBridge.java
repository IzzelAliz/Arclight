package io.izzel.arclight.common.bridge.core.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface EnchantmentMenuBridge {

    default float bridge$forge$getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return state.is(BlockTags.ENCHANTMENT_POWER_PROVIDER) ? 1 : 0;
    }

    default int bridge$forge$onEnchantmentLevelSet(Level level, BlockPos pos, int enchantRow, int power, ItemStack itemStack, int enchantmentLevel) {
        return enchantmentLevel;
    }
}
