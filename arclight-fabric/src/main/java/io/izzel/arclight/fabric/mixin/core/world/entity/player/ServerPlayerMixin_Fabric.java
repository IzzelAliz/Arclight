package io.izzel.arclight.fabric.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Fabric extends PlayerMixin_Fabric implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract ServerLevel serverLevel();
    @Shadow public boolean isChangingDimension;
    @Shadow public boolean wonGame;
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow private boolean seenCredits;
    @Shadow public abstract CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel arg);
    @Shadow @Final public MinecraftServer server;
    @Shadow @Nullable private Vec3 enteredNetherPosition;
    @Shadow protected abstract void createEndPlatform(ServerLevel arg, BlockPos arg2);
    @Shadow public abstract void setServerLevel(ServerLevel arg);
    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel arg);
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow public int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;
    // @formatter:on
}
