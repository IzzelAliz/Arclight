package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TNTBlock;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TNTBlock.class)
public class TNTBlockMixin {

    @Inject(method = "onProjectileCollision", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/block/TNTBlock;catchFire(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;Lnet/minecraft/entity/LivingEntity;)V"))
    public void arclight$entityChangeBlock(World worldIn, BlockState state, BlockRayTraceResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent(projectile, hit.getPos(), Blocks.AIR.getDefaultState()).isCancelled()) {
            ci.cancel();
        }
    }
}
