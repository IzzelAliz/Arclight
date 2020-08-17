package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends EntityMixin {

    @Shadow private BlockState fallTile;

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$entityChangeBlock(CallbackInfo ci, Block block, BlockPos pos) {
        if (CraftEventFactory.callEntityChangeBlockEvent((FallingBlockEntity) (Object) this, pos, this.fallTile).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onLivingFall", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private void arclight$damageSource(float distance, float damageMultiplier, CallbackInfoReturnable<Boolean> cir) {
        CraftEventFactory.entityDamage = (FallingBlockEntity) (Object) this;
    }

    @Inject(method = "onLivingFall", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private void arclight$damageSourceReset(float distance, float damageMultiplier, CallbackInfoReturnable<Boolean> cir) {
        CraftEventFactory.entityDamage = null;
    }
}
