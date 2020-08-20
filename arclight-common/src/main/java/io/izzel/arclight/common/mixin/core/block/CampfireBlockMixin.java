package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public class CampfireBlockMixin {

    @Inject(method = "onProjectileCollision", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$onFire(World worldIn, BlockState state, BlockRayTraceResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (CraftEventFactory.callBlockIgniteEvent(worldIn, hit.getPos(), projectile).isCancelled()) {
            ci.cancel();
        }
    }
}
