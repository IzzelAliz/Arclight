package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DaylightDetectorBlock.class)
public class DaylightDetectorBlockMixin {
    @WrapOperation(method = "updateSignalStrength", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean arclight$blockRedstone(Level level, BlockPos pos, BlockState proposed, int flags,
            Operation<Boolean> original, BlockState previous, Level sourceLevel, BlockPos sourcePos) {
        int power = CraftEventFactory.callRedstoneChange(level, pos,
            previous.getValue(DaylightDetectorBlock.POWER), proposed.getValue(DaylightDetectorBlock.POWER)).getNewCurrent();
        return original.call(level, pos, proposed.setValue(DaylightDetectorBlock.POWER, power), flags);
    }
}
