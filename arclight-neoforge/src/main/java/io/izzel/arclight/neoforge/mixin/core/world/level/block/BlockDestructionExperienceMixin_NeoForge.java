package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockDestructionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockDestructionExperienceMixin_NeoForge implements BlockDestructionBridge {

    @Override
    public boolean bridge$usesNeoForgeDestructionDrops() {
        return true;
    }

    @Override
    public int bridge$getDestructionExperience(BlockState state, ServerLevel level, BlockPos pos, Entity breaker) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        return EnchantmentHelper.processBlockExperience(level, ItemStack.EMPTY,
            state.getExpDrop(level, pos, blockEntity, breaker, ItemStack.EMPTY));
    }
}
