package io.izzel.arclight.common.mod.server.api;

import io.izzel.arclight.api.TickingTracker;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class DefaultTickingTracker implements TickingTracker {

    @Nullable
    @Override
    public Object getTickingSource() {
        Entity entity = getTickingEntity();
        if (entity != null) {
            return entity;
        }
        TileState tileState = getTickingBlockEntity();
        if (tileState != null) {
            return tileState;
        }
        Block block = getTickingBlock();
        if (block != null) {
            return block;
        }
        return null;
    }

    @Nullable
    @Override
    public Entity getTickingEntity() {
        net.minecraft.entity.Entity tickingEntity = ArclightCaptures.getTickingEntity();
        if (tickingEntity != null) {
            return ((EntityBridge) tickingEntity).bridge$getBukkitEntity();
        }
        return null;
    }

    @Nullable
    @Override
    public Block getTickingBlock() {
        ServerWorld world = ArclightCaptures.getTickingWorld();
        BlockPos pos = ArclightCaptures.getTickingPosition();
        if (world != null && pos != null) {
            return CraftBlock.at(world, pos);
        }
        return null;
    }

    @Nullable
    @Override
    public TileState getTickingBlockEntity() {
        TileEntity tileEntity = ArclightCaptures.getTickingTileEntity();
        if (tileEntity != null) {
            World world = tileEntity.getWorld();
            if (world != null) {
                CraftBlock block = CraftBlock.at(world, tileEntity.getPos());
                BlockState state = block.getState();
                if (state instanceof TileState) {
                    return (TileState) state;
                }
            }
        }
        return null;
    }
}
