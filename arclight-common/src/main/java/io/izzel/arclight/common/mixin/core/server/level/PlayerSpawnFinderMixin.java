package io.izzel.arclight.common.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerSpawnFinder.class)
public abstract class PlayerSpawnFinderMixin {

    @ModifyExpressionValue(method = "findSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Ljava/util/concurrent/CompletableFuture;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/WorldData;getGameType()Lnet/minecraft/world/level/GameType;"))
    private static GameType arclight$worldGameMode(GameType original, ServerLevel level, BlockPos spawnSuggestion) {
        return level.serverLevelData.getGameType();
    }
}
