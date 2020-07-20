package io.izzel.arclight.common.bridge.world.server;

import net.minecraft.entity.EntityClassification;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorldLightManager;

import java.io.IOException;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();

    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ServerWorldLightManager bridge$getLightManager();

    void bridge$worldNaturalSpawn(EntityClassification classification, World worldIn, Chunk chunk, BlockPos pos);
}
