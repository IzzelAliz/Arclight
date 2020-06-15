package io.izzel.arclight.impl.mixin.v1_14.core.server.management;

import io.izzel.arclight.common.bridge.server.management.PlayerListBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

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
}
