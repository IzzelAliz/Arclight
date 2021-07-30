package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(InteractWithDoor.class)
public abstract class InteractWithDoorMixin {

    // @formatter:off
    @Shadow @Nullable private Node lastCheckedNode;
    @Shadow protected abstract void rememberDoorToClose(ServerLevel p_242301_1_, LivingEntity p_242301_2_, BlockPos p_242301_3_);
    @Shadow public static void closeDoorsThatIHaveOpenedOrPassedThrough(ServerLevel p_242294_0_, LivingEntity p_242294_1_, @org.jetbrains.annotations.Nullable Node p_242294_2_, @org.jetbrains.annotations.Nullable Node p_242294_3_) { }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void start(ServerLevel worldIn, LivingEntity entityIn, long gameTimeIn) {
        Path path = entityIn.getBrain().getMemory(MemoryModuleType.PATH).get();
        this.lastCheckedNode = path.getNextNode();
        Node pathpoint = path.getPreviousNode();
        Node pathpoint1 = path.getNextNode();
        BlockPos blockpos = pathpoint.asBlockPos();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        if (blockstate.is(BlockTags.WOODEN_DOORS)) {
            DoorBlock doorblock = (DoorBlock) blockstate.getBlock();
            if (!doorblock.isOpen(blockstate)) {
                EntityInteractEvent event = new org.bukkit.event.entity.EntityInteractEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(), CraftBlock.at(entityIn.level, blockpos));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                doorblock.setOpen(entityIn, worldIn, blockstate, blockpos, true);
            }

            this.rememberDoorToClose(worldIn, entityIn, blockpos);
        }

        BlockPos blockpos1 = pathpoint1.asBlockPos();
        BlockState blockstate1 = worldIn.getBlockState(blockpos1);
        if (blockstate1.is(BlockTags.WOODEN_DOORS)) {
            DoorBlock doorblock1 = (DoorBlock) blockstate1.getBlock();
            if (!doorblock1.isOpen(blockstate1)) {
                EntityInteractEvent event = new org.bukkit.event.entity.EntityInteractEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(), CraftBlock.at(entityIn.level, blockpos));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                doorblock1.setOpen(entityIn, worldIn, blockstate1, blockpos1, true);
                this.rememberDoorToClose(worldIn, entityIn, blockpos1);
            }
        }

        closeDoorsThatIHaveOpenedOrPassedThrough(worldIn, entityIn, pathpoint, pathpoint1);
    }
}
