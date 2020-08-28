package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class AbstractBlock_AbstractBlockStateMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"))
    private void arclight$captureBlockCollide(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        ArclightCaptures.captureDamageEventBlock(pos);
    }

    @Inject(method = "onEntityCollision", at = @At("RETURN"))
    private void arclight$resetBlockCollide(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        ArclightCaptures.captureDamageEventBlock(null);
    }
}
