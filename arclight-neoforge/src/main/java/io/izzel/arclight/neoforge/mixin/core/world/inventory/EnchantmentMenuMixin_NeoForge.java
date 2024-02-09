package io.izzel.arclight.neoforge.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.EnchantmentMenuBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin_NeoForge implements EnchantmentMenuBridge {

    @Override
    public float bridge$forge$getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return state.getBlock().getEnchantPowerBonus(state, level, pos);
    }

    @Override
    public int bridge$forge$onEnchantmentLevelSet(Level level, BlockPos pos, int enchantRow, int power, ItemStack itemStack, int enchantmentLevel) {
        return EventHooks.onEnchantmentLevelSet(level, pos, enchantRow, power, itemStack, enchantmentLevel);
    }
}
