package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import com.google.common.collect.Sets;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.InteractWithDoorTask;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Set;

@Mixin(InteractWithDoorTask.class)
public class InteractWithDoorTaskMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void func_220434_a(ServerWorld serverWorld, List<BlockPos> blockPosList, Set<BlockPos> blockPosSet, int i, LivingEntity livingEntity, Brain<?> brain) {
        blockPosSet.forEach((blockPos) -> {
            int j = blockPosList.indexOf(blockPos);
            BlockState blockstate = serverWorld.getBlockState(blockPos);
            Block block = blockstate.getBlock();
            if (BlockTags.WOODEN_DOORS.contains(block) && block instanceof DoorBlock) {
                boolean flag = j >= i;

                // CraftBukkit start - entities opening doors
                EntityInteractEvent event = new EntityInteractEvent(((LivingEntityBridge) livingEntity).bridge$getBukkitEntity(), CraftBlock.at(livingEntity.world, blockPos));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                // CaftBukkit end

                ((DoorBlock) block).toggleDoor(serverWorld, blockPos, flag);
                GlobalPos globalpos = GlobalPos.of(serverWorld.getDimension().getType(), blockPos);
                if (!brain.getMemory(MemoryModuleType.field_225462_q).isPresent() && flag) {
                    brain.setMemory(MemoryModuleType.field_225462_q, Sets.newHashSet(globalpos));
                } else {
                    brain.getMemory(MemoryModuleType.field_225462_q).ifPresent((globalPosSet) -> {
                        if (flag) {
                            globalPosSet.add(globalpos);
                        } else {
                            globalPosSet.remove(globalpos);
                        }

                    });
                }
            }

        });
        InteractWithDoorTask.func_225449_a(serverWorld, blockPosList, i, livingEntity, brain);
    }
}
