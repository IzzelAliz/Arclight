package io.izzel.arclight.common.mixin.core.world.level.redstone;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NeighborUpdater.class)
public interface NeighborUpdaterMixin {

    @Decorate(method = "executeUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;handleNeighborChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V"))
    private static void arclight$blockPhysicsEvent(BlockState instance, Level level, BlockPos pos, Block block, BlockPos source, boolean b) throws Throwable {
        var cworld = level.bridge$getWorld();
        if (cworld != null) {
            BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(level, pos), CraftBlockData.fromData(instance), CraftBlock.at(level, source));
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                DecorationOps.cancel().invoke();
                return;
            }
        }
        DecorationOps.callsite().invoke(instance, level, pos, block, source, b);
    }

    @Decorate(method = "executeUpdate", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
    private static void arclight$setLastPhysicsProblem(Level level, BlockState instance, BlockPos pos, @Local(ordinal = -1) Throwable t) {
        if (t instanceof StackOverflowError) {
            ((WorldBridge) level).bridge$setLastPhysicsProblem(pos.immutable());
        }
    }
}
