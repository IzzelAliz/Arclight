package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CampfireBlock.class)
public class CampfireBlockMixin {

    @Inject(method = "onProjectileCollision", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$onFire(World worldIn, BlockState state, BlockRayTraceResult hit, Entity projectile, CallbackInfo ci, AbstractArrowEntity arrowEntity, BlockPos blockPos) {
        if (CraftEventFactory.callBlockIgniteEvent(worldIn, blockPos, arrowEntity).isCancelled()) {
            ci.cancel();
        }
    }
}
