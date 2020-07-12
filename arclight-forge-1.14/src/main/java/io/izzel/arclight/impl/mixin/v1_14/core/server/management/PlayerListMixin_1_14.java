package io.izzel.arclight.impl.mixin.v1_14.core.server.management;

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
public abstract class PlayerListMixin_1_14 implements PlayerListBridge {

    @Override
    public boolean bridge$worldNoCollision(ServerWorld world, Entity entity) {
        return world.areCollisionShapesEmpty(entity);
    }

    @Override
    public void bridge$setSpawnPoint(ServerPlayerEntity player, BlockPos pos, boolean flag, DimensionType type, boolean flag1) {
        player.setSpawnPoint(pos, flag, type);
    }

    @Override
    public SRespawnPacket bridge$respawnPacket(DimensionType type, long seed, WorldType worldType, GameType gameType) {
        return new SRespawnPacket(type, worldType, gameType);
    }

    @Redirect(method = "initializeConnectionToPlayer", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SJoinGamePacket"))
    private SJoinGamePacket arclight$spawnPacket(int playerId, GameType gameType, boolean hardcoreMode, DimensionType dimensionType, int maxPlayers, WorldType worldType, int viewDistance, boolean reducedDebugInfo, NetworkManager netManager, ServerPlayerEntity playerIn) {
        return new SJoinGamePacket(playerId, gameType, hardcoreMode, ((DimensionTypeBridge) dimensionType).bridge$getType(), maxPlayers, worldType, ((ServerWorldBridge) playerIn.func_71121_q()).bridge$spigotConfig().viewDistance, reducedDebugInfo);
    }
}
