package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.TripWireHookBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TripWireHookBlock.class)
public class TripWireHookBlockMixin {

    @Inject(method = "calculateState", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/block/TripWireHookBlock;playSound(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ZZZZ)V"))
    public void arclight$blockRedstone(World worldIn, BlockPos pos, BlockState hookState, boolean attaching, boolean shouldNotifyNeighbours, int searchRange, BlockState state, CallbackInfo ci) {
        BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(worldIn, pos), 15, 0);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getNewCurrent() > 0) {
            ci.cancel();
        }
    }
}
