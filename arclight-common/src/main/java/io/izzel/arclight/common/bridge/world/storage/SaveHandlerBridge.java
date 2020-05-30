package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.nbt.CompoundNBT;

import java.io.File;
import java.util.UUID;

public interface SaveHandlerBridge {

    String[] bridge$getSeenPlayers();

    UUID bridge$getUUID();

    File bridge$getPlayerDir();

    CompoundNBT bridge$getPlayerData(String uuid);
}
