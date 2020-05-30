package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluidBlock.class)
public class FlowingFluidBlockMixin {

    private transient boolean arclight$fizz = true;

    @Redirect(method = "reactWithNeighbors", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$blockForm(World world, BlockPos pos, BlockState state) {
        return arclight$fizz = CraftEventFactory.handleBlockFormEvent(world, pos, state);
    }

    @Inject(method = "triggerMixEffects", cancellable = true, at = @At("HEAD"))
    public void arclight$fizz(IWorld worldIn, BlockPos pos, CallbackInfo ci) {
        if (!arclight$fizz) {
            ci.cancel();
        }
    }
}
