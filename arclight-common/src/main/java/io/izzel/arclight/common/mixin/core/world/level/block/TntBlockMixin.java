package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntBlock.class)
public class TntBlockMixin {

    @Inject(method = "onProjectileHit", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;onCaughtFire(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V"))
    public void arclight$entityChangeBlock(Level worldIn, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent(projectile, hit.getBlockPos(), Blocks.AIR.defaultBlockState()).isCancelled()) {
            ci.cancel();
        }
    }
}
