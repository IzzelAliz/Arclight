package io.izzel.arclight.common.mixin.vanilla.world.level;

import io.izzel.arclight.common.mod.server.world.WorldCreationContext;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelWorldGenMixin_Vanilla {
    @Decorate(method = "getSeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldGenSettings()Lnet/minecraft/world/level/levelgen/WorldGenSettings;"))
    private WorldGenSettings arclight$worldSeedSettings(MinecraftServer server) throws Throwable {
        var original = (WorldGenSettings) DecorationOps.callsite().invoke(server);
        var own = WorldCreationContext.lookup((ServerLevel) (Object) this);
        return own != null ? own : original;
    }
}
