package io.izzel.arclight.bridge.server.management;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;

import java.util.List;

public interface PlayerListBridge {

    void bridge$setPlayers(List<ServerPlayerEntity> players);

    List<ServerPlayerEntity> bridge$getPlayers();

    CraftServer bridge$getCraftServer();
}
