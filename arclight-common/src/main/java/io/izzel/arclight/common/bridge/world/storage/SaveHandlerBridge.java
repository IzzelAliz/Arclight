package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.util.UUID;

public interface SaveHandlerBridge {

    String[] bridge$getSeenPlayers();

    UUID bridge$getUUID(ServerWorld world);

    File bridge$getPlayerDir();

    CompoundNBT bridge$getPlayerData(String uuid);
}
