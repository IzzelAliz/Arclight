package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.InteractWithDoorTask;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(InteractWithDoorTask.class)
public abstract class InteractWithDoorTaskMixin {

    // @formatter:off
    @Shadow @Nullable private PathPoint field_242292_b;
    @Shadow protected abstract void func_242301_c(ServerWorld p_242301_1_, LivingEntity p_242301_2_, BlockPos p_242301_3_);
    @Shadow public static void func_242294_a(ServerWorld p_242294_0_, LivingEntity p_242294_1_, @org.jetbrains.annotations.Nullable PathPoint p_242294_2_, @org.jetbrains.annotations.Nullable PathPoint p_242294_3_) { }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void startExecuting(ServerWorld worldIn, LivingEntity entityIn, long gameTimeIn) {
        Path path = entityIn.getBrain().getMemory(MemoryModuleType.PATH).get();
        this.field_242292_b = path.func_237225_h_();
        PathPoint pathpoint = path.func_242950_i();
        PathPoint pathpoint1 = path.func_237225_h_();
        BlockPos blockpos = pathpoint.func_224759_a();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        if (blockstate.isIn(BlockTags.WOODEN_DOORS)) {
            DoorBlock doorblock = (DoorBlock) blockstate.getBlock();
            if (!doorblock.isOpen(blockstate)) {
                EntityInteractEvent event = new org.bukkit.event.entity.EntityInteractEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(), CraftBlock.at(entityIn.world, blockpos));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                doorblock.openDoor(worldIn, blockstate, blockpos, true);
            }

            this.func_242301_c(worldIn, entityIn, blockpos);
        }

        BlockPos blockpos1 = pathpoint1.func_224759_a();
        BlockState blockstate1 = worldIn.getBlockState(blockpos1);
        if (blockstate1.isIn(BlockTags.WOODEN_DOORS)) {
            DoorBlock doorblock1 = (DoorBlock) blockstate1.getBlock();
            if (!doorblock1.isOpen(blockstate1)) {
                // todo check this blockpos1
                EntityInteractEvent event = new org.bukkit.event.entity.EntityInteractEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(), CraftBlock.at(entityIn.world, blockpos1));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                doorblock1.openDoor(worldIn, blockstate1, blockpos1, true);
                this.func_242301_c(worldIn, entityIn, blockpos1);
            }
        }

        func_242294_a(worldIn, entityIn, pathpoint, pathpoint1);
    }
}
