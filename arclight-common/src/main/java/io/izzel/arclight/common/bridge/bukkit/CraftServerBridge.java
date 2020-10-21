package io.izzel.arclight.common.bridge.bukkit;

import net.minecraft.server.management.PlayerList;
import net.minecraft.world.server.ServerWorld;

public interface CraftServerBridge {

    void bridge$setPlayerList(PlayerList playerList);

    void bridge$removeWorld(ServerWorld world);
}
