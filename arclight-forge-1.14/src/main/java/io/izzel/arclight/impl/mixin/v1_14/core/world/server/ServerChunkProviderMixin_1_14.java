package io.izzel.arclight.impl.mixin.v1_14.core.world.server;

import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.spawner.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkProviderMixin_1_14 implements ServerChunkProviderBridge {

    @Override
    public void bridge$worldNaturalSpawn(EntityClassification classification, World worldIn, Chunk chunk, BlockPos pos) {
        WorldEntitySpawner.performNaturalSpawning(classification, worldIn, chunk, pos);
    }
}
