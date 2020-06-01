package io.izzel.arclight.common.mixin.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {

    // @formatter:off
    @Shadow @Final private boolean isSticky;
    // @formatter:on

    @Inject(method = "checkForMove", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addBlockEvent(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V"))
    public void arclight$pistonRetract(World worldIn, BlockPos pos, BlockState state, CallbackInfo ci, Direction direction) {
        if (!this.isSticky) {
            Block block = CraftBlock.at(worldIn, pos);
            BlockPistonRetractEvent event = new BlockPistonRetractEvent(block, ImmutableList.of(), CraftBlock.notchToBlockFace(direction));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
