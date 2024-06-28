package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CommandBlock.class)
public abstract class CommandBlockMixin {

    @Decorate(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean arclight$blockRedstone(Level instance, BlockPos pos) throws Throwable {
        var flag = (boolean) DecorationOps.callsite().invoke(instance, pos);
        var blockEntity = ((CommandBlockEntity) instance.getBlockEntity(pos));
        boolean flag1 = blockEntity.isPowered();
        org.bukkit.block.Block bukkitBlock = CraftBlock.at(instance, pos);
        int old = flag1 ? 15 : 0;
        int current = flag ? 15 : 0;
        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, old, current);
        Bukkit.getPluginManager().callEvent(eventRedstone);
        return eventRedstone.getNewCurrent() > 0;
    }
}
