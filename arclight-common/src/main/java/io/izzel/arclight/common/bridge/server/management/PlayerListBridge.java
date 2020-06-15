package io.izzel.arclight.common.bridge.server.management;

import net.minecraft.entity.Entity;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.network.login.ServerLoginNetHandler;
import org.bukkit.craftbukkit.v.CraftServer;

import java.net.SocketAddress;
import java.util.List;

public interface PlayerListBridge {

    void bridge$setPlayers(List<ServerPlayerEntity> players);

    List<ServerPlayerEntity> bridge$getPlayers();

    CraftServer bridge$getCraftServer();

    ServerPlayerEntity bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginNetHandler handler);

    boolean bridge$worldNoCollision(ServerWorld world, Entity entity);

    void bridge$setSpawnPoint(ServerPlayerEntity player, BlockPos pos, boolean flag, DimensionType type, boolean flag1);

    SRespawnPacket bridge$respawnPacket(DimensionType type, long seed, WorldType worldType, GameType gameType);
}
