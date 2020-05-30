package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoublePlantBlock.class)
public class DoublePlantBlockMixin {

    @Inject(method = "onBlockHarvested", cancellable = true, at = @At("HEAD"))
    public void arclight$blockPhysics(World worldIn, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (CraftEventFactory.callBlockPhysicsEvent(worldIn, pos).isCancelled()) {
            ci.cancel();
        }
    }
}
