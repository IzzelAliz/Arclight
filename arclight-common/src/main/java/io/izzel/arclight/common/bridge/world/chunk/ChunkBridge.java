package io.izzel.arclight.common.bridge.world.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;

public interface ChunkBridge {

    Chunk bridge$getBukkitChunk();

    void bridge$setBukkitChunk(Chunk chunk);

    BlockState bridge$setType(BlockPos pos, BlockState state, boolean isMoving, boolean doPlace);

    boolean bridge$isMustNotSave();

    void bridge$setMustNotSave(boolean mustNotSave);

    boolean bridge$isNeedsDecoration();

    void bridge$loadCallback();

    void bridge$unloadCallback();

    CraftPersistentDataContainer bridge$getPersistentContainer();
}
