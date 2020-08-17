package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LilyPadBlock.class)
public class LilyPadBlockMixin {

    @Inject(method = "onEntityCollision", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;destroyBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"))
    public void arclight$entityChangeBlock(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent(entityIn, pos, Blocks.AIR.getDefaultState()).isCancelled()) {
            ci.cancel();
        }
    }
}
