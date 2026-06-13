package io.izzel.arclight.common.mixin.core.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerSpawnFinder.class)
public interface PlayerSpawnFinderAccessor {

    @Invoker("getOverworldRespawnPos")
    static BlockPos arclight$getOverworldRespawnPos(ServerLevel level, int x, int z) {
        throw new AssertionError();
    }
}
