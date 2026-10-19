package io.izzel.arclight.common.mixin.core.server.network.config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.core.server.network.config.PrepareSpawnTaskBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelData.RespawnData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

/**
 * The binding is task-local and is read before
 * PrepareSpawnTask.start chooses a spawn level, finder origin, or rotation.
 */
@Mixin(PrepareSpawnTask.class)
public abstract class PrepareSpawnTaskMixin implements PrepareSpawnTaskBridge {

    @Shadow @Final private NameAndId nameAndId;

    @Unique
    @Nullable
    private ServerPlayer arclight$spawnPlayer;

    @Override
    @Unique
    public void bridge$setSpawnPlayer(ServerPlayer player) {
        this.arclight$spawnPlayer = player;
    }

    @WrapOperation(method = "start(Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ServerLevelData;getRespawnData()Lnet/minecraft/world/level/storage/LevelData$RespawnData;"))
    private RespawnData arclight$useBoundPlayersLevelData(ServerLevelData overworldData,
                                                           Operation<RespawnData> original) {
        // Keep the original vanilla/NeoForge getter's invocation and side effects exactly once.
        RespawnData fallback = original.call(overworldData);

        ServerPlayer candidate = this.arclight$spawnPlayer;
        if (candidate == null) {
            return fallback;
        }
        GameProfile profile = candidate.getGameProfile();
        if (!Objects.equals(profile.id(), this.nameAndId.id())
            || !Objects.equals(profile.name(), this.nameAndId.name())) {
            return fallback;
        }

        // Do not use ServerLevel.getRespawnData(): its current native implementation is global.
        // Level.getLevelData() is the per-level data object; preserve all RespawnData fields.
        return candidate.level().getLevelData().getRespawnData();
    }
}
