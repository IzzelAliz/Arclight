package io.izzel.arclight.common.mixin.core.world.level;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.izzel.arclight.common.bridge.core.world.level.block.BlockDestructionBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelBlockDestroyEventMixin {

    @Decorate(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", inject = true,
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"), require = 1, allow = 1)
    private void arclight$blockDestroy(BlockPos pos, boolean dropResources, Entity breaker, int updateLimit,
                                       @Local(ordinal = 0) BlockState state, @Local(ordinal = 0) FluidState fluid,
                                       @Local(allocate = "arclightBlockDestroyEvent") BlockDestroyEvent event,
                                       @Local(allocate = "arclightBlockDestroyExperience") int experience) throws Throwable {
        event = null;
        experience = 0;
        if ((Object) this instanceof ServerLevel level && DistValidate.isValid(level)) {
            experience = ((BlockDestructionBridge) state.getBlock()).bridge$getDestructionExperience(state, level, pos, breaker);
            if (BlockDestroyEvent.getHandlerList().getRegisteredListeners().length > 0) {
                event = new BlockDestroyEvent(CraftBlock.at(level, pos), CraftBlockData.createData(fluid.createLegacyBlock()),
                    CraftBlockData.createData(state), experience, dropResources);
                if (!event.callEvent()) {
                    DecorationOps.cancel().invoke(false);
                    return;
                }
                dropResources = event.willDrop();
                experience = event.getExpToDrop();
            }
        }
        DecorationOps.blackhole().invoke(dropResources, event, experience);
    }

    @Decorate(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), require = 1, allow = 1)
    private void arclight$blockDestroyEffect(Level level, int effect, BlockPos pos, int data,
                                             @Local(allocate = "arclightBlockDestroyEvent") BlockDestroyEvent event) throws Throwable {
        if (event != null) {
            if (!event.playEffect()) {
                return;
            }
            data = Block.getId(((CraftBlockData) event.getEffectBlock()).getState());
        }
        // The original BaseFireBlock exclusion remains outside this callsite.
        DecorationOps.callsite().invoke(level, effect, pos, data);
    }

    @Decorate(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"), require = 1, allow = 1)
    private void arclight$blockDestroyDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                            Entity breaker, ItemStack tool,
                                            @Local(allocate = "arclightBlockDestroyExperience") int experience) throws Throwable {
        var previous = io.izzel.arclight.common.mod.util.ArclightCaptures.blockDestruction;
        BlockDestruction context = level instanceof ServerLevel serverLevel && DistValidate.isValid(level)
            ? BlockDestruction.blockDestroy(serverLevel, pos, experience) : null;
        if (context != null) io.izzel.arclight.common.mod.util.ArclightCaptures.blockDestruction = context;
        try {
            DecorationOps.callsite().invoke(state, level, pos, blockEntity, breaker, tool);
        } finally {
            if (context != null) {
                io.izzel.arclight.common.mod.util.ArclightCaptures.blockDestruction = previous;
            }
        }
    }
}
