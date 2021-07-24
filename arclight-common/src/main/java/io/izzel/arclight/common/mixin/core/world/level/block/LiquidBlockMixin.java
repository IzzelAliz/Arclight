package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public class LiquidBlockMixin {

    private transient boolean arclight$fizz = true;

    @Redirect(method = "shouldSpreadLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$blockForm(Level world, BlockPos pos, BlockState state) {
        return arclight$fizz = CraftEventFactory.handleBlockFormEvent(world, pos, state);
    }

    @Inject(method = "fizz", cancellable = true, at = @At("HEAD"))
    public void arclight$fizz(LevelAccessor worldIn, BlockPos pos, CallbackInfo ci) {
        if (!arclight$fizz) {
            ci.cancel();
        }
    }
}
