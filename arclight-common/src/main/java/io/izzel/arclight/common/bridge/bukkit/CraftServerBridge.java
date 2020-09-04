package io.izzel.arclight.common.bridge.bukkit;

import net.minecraft.server.management.PlayerList;

public interface CraftServerBridge {

    void bridge$setPlayerList(PlayerList playerList);
}
