package io.izzel.arclight.common.mixin.v1_15.server.management;

import io.izzel.arclight.common.bridge.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin_1_15 implements PlayerListBridge {

    @Override
    public boolean bridge$worldNoCollision(ServerWorld world, Entity entity) {
        return world.hasNoCollisions(entity);
    }

    @Override
    public void bridge$setSpawnPoint(ServerPlayerEntity player, BlockPos pos, boolean flag, DimensionType type, boolean flag1) {
        player.setSpawnPoint(pos, flag, flag1, type);
    }

    @Override
    public SRespawnPacket bridge$respawnPacket(DimensionType type, long seed, WorldType worldType, GameType gameType) {
        return new SRespawnPacket(type, seed, worldType, gameType);
    }

    @Redirect(method = "initializeConnectionToPlayer", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SJoinGamePacket"))
    private SJoinGamePacket arclight$spawnPacket(int playerId, GameType gameType, long hashedSeed, boolean hardcoreMode, DimensionType dimensionType, int maxPlayers, WorldType worldType, int viewDistance, boolean reducedDebugInfo, boolean enableRespawnScreen, NetworkManager netManager, ServerPlayerEntity playerIn) {
        return new SJoinGamePacket(playerId, gameType, hashedSeed, hardcoreMode, ((DimensionTypeBridge) dimensionType).bridge$getType(), maxPlayers, worldType, ((ServerWorldBridge) playerIn.getServerWorld()).bridge$spigotConfig().viewDistance, reducedDebugInfo, enableRespawnScreen);
    }
}
