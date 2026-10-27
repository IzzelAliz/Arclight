package io.izzel.arclight.common.bridge.core.server.network.config;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public interface PrepareSpawnTaskLocationEventBridge {

    void bridge$bindSpawnLocationConnection(PlayerConfigurationConnection connection, Connection network, @Nullable ServerPlayer player);

    PlayerConfigurationConnection bridge$getSpawnLocationConnection();

    @Nullable ServerPlayer bridge$getSpawnLocationPlayer();

    Connection bridge$getSpawnLocationNetwork();

    boolean bridge$isNewSpawnPlayer();
}
