package io.izzel.arclight.common.mixin.core.world.level.block.state;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockBehaviour_BlockStateBaseMixin {

    @WrapMethod(method = "entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V")
    private void arclight$captureBlockCollide(Level worldIn, BlockPos pos, Entity entityIn, InsideBlockEffectApplier effectApplier, boolean flag, Operation<Void> original) {
        try {
            ArclightCaptures.captureDamageEventBlock(pos);
            original.call(worldIn, pos, entityIn, effectApplier, flag);
        } finally {
            ArclightCaptures.captureDamageEventBlock(null);
        }
    }
}
