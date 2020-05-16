package io.izzel.arclight.bridge.world.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Chunk;

public interface ChunkBridge {

    Chunk bridge$getBukkitChunk();

    BlockState bridge$setType(BlockPos pos, BlockState state, boolean isMoving, boolean doPlace);

    boolean bridge$isMustNotSave();

    void bridge$setMustNotSave(boolean mustNotSave);

    boolean bridge$isNeedsDecoration();

    void bridge$loadCallback();

    void bridge$unloadCallback();
}
