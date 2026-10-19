package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrapDoorBlock.class)
public class TrapDoorBlockMixin {
    // The outer POWERED comparison must use the native signal, not the plugin's replacement.
    @Inject(method = "neighborChanged", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/TrapDoorBlock;OPEN:Lnet/minecraft/world/level/block/state/properties/BooleanProperty;", ordinal = 0))
    private void arclight$blockRedstone(BlockState state, Level level, BlockPos pos, Block block,
            Orientation orientation, boolean movedByPiston, CallbackInfo ci,
            @Local(ordinal = 1) LocalBooleanRef signal) {
        var craftBlock = CraftBlock.at(level, pos);
        int power = craftBlock.getBlockPower();
        int oldPower = state.getValue(TrapDoorBlock.OPEN) ? 15 : 0;
        if (oldPower == 0 ^ power == 0 || block.defaultBlockState().isSignalSource()) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(craftBlock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            signal.set(event.getNewCurrent() > 0);
        }
    }
}
