package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BasePressurePlateBlock.class)
public abstract class BasePressurePlateBlockMixin {

    // @formatter:off
    @Shadow protected abstract int getSignalStrength(Level worldIn, BlockPos pos);
    @Shadow @Final protected static AABB TOUCH_AABB;
    // @formatter:on

    @Redirect(method = "checkPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BasePressurePlateBlock;getSignalStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int arclight$blockRedstone(BasePressurePlateBlock abstractPressurePlateBlock, Level worldIn, BlockPos pos, Entity entity, Level world, BlockPos blockPos, BlockState state, int oldRedstoneStrength) {
        int newStrength = this.getSignalStrength(worldIn, pos);
        boolean flag = oldRedstoneStrength > 0;
        boolean flag1 = newStrength > 0;

        if (flag != flag1 && DistValidate.isValid(world)) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(worldIn, blockPos), oldRedstoneStrength, newStrength);
            Bukkit.getPluginManager().callEvent(event);
            newStrength = event.getNewCurrent();
        }
        return newStrength;
    }
}
