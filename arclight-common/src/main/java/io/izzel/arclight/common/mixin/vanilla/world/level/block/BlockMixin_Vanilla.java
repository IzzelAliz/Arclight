package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class BlockMixin_Vanilla implements BlockBridge {

    // @formatter:off
    @Shadow public void fallOn(Level level, BlockState blockState, BlockPos blockPos, Entity entity, double d) {}
    // @formatter:on

    /**
     * Raw vanilla/Fabric {@link Block#playerDestroy} reaches native EXP producers
     * through {@code Block.dropResources -> BlockState.spawnAfterBreak(..., true)}.
     * The outer destroyBlock scope emits the final Bukkit value, so suppress only
     * that native boolean branch and retain native item drops. NeoForge replaces
     * this call with CommonHooks.handleBlockDrops and must not load this mixin.
     */
    @Decorate(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"))
    private static void arclight$suppressNativeBlockBreakExperience(BlockState state, ServerLevel world, BlockPos pos, ItemStack tool, boolean dropExperience) throws Throwable {
        DecorationOps.callsite().invoke(state, world, pos, tool,
            ArclightCaptures.shouldSuppressVanillaBlockBreakExperience(world, pos) ? false : dropExperience);
    }
}
