package io.izzel.arclight.common.mixin.core.server.network.config;

import io.izzel.arclight.common.bridge.core.server.network.config.PrepareSpawnTaskLocationEventBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;
import java.util.Optional;

@Mixin(PrepareSpawnTask.class)
public abstract class PrepareSpawnTaskLocationEventMixin implements PrepareSpawnTaskLocationEventBridge {

    @Unique private PlayerConfigurationConnection arclight$spawnLocationConnection;
    @Unique private @Nullable ServerPlayer arclight$spawnLocationPlayer;
    @Unique private Connection arclight$spawnLocationNetwork;
    @Unique private boolean arclight$newSpawnPlayer;

    @Override
    public void bridge$bindSpawnLocationConnection(PlayerConfigurationConnection connection, Connection network, @Nullable ServerPlayer player) {
        this.arclight$spawnLocationConnection = Objects.requireNonNull(connection, "configuration connection");
        this.arclight$spawnLocationPlayer = player;
        this.arclight$spawnLocationNetwork = Objects.requireNonNull(network, "network connection");
    }

    @Override
    public PlayerConfigurationConnection bridge$getSpawnLocationConnection() {
        return Objects.requireNonNull(this.arclight$spawnLocationConnection, "Unbound spawn-location task");
    }

    @Override
    public @Nullable ServerPlayer bridge$getSpawnLocationPlayer() {
        return this.arclight$spawnLocationPlayer;
    }

    @Override
    public Connection bridge$getSpawnLocationNetwork() {
        return Objects.requireNonNull(this.arclight$spawnLocationNetwork, "Unbound spawn-location network");
    }

    @Override
    public boolean bridge$isNewSpawnPlayer() {
        return this.arclight$newSpawnPlayer;
    }

    @Decorate(method = "start(Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;loadPlayerData(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;"))
    private Optional<CompoundTag> arclight$captureNewSpawnPlayer(PlayerList list, NameAndId profile) throws Throwable {
        var data = (Optional<CompoundTag>) DecorationOps.callsite().invoke(list, profile);
        this.arclight$newSpawnPlayer = data.isEmpty();
        return data;
    }
}
