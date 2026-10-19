package io.izzel.arclight.common.mixin.core.world.level.chunk.status;

import io.izzel.arclight.common.mod.server.world.WorldCreationContext;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {

    @Decorate(method = "generateStructureStarts", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldGenSettings()Lnet/minecraft/world/level/levelgen/WorldGenSettings;"))
    private static WorldGenSettings arclight$useLevelData(MinecraftServer instance, @Local(ordinal = -1) ServerLevel level) throws Throwable {
        var original = (WorldGenSettings) DecorationOps.callsite().invoke(instance);
        var own = WorldCreationContext.lookup(level);
        return own != null ? own : original;
    }
}
