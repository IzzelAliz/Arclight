package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.nbt.CompoundNBT;

import java.io.File;

public interface PlayerDataBridge {

    File bridge$getPlayerDir();

    CompoundNBT bridge$getPlayerData(String uuid);
}
