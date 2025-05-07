package io.izzel.arclight.common.mixin.core.world.level.chunk.status;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {

    @Decorate(method = "generateStructureStarts", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private static WorldData arclight$useLevelData(MinecraftServer instance, @Local(ordinal = -1) ServerLevel level) throws Throwable {
        return level.serverLevelData instanceof WorldData custom ? custom : (WorldData) DecorationOps.callsite().invoke(instance);
    }
}
