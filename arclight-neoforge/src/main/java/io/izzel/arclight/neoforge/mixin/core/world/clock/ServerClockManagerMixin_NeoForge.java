package io.izzel.arclight.neoforge.mixin.core.world.clock;

import io.izzel.arclight.common.bridge.core.world.clock.ServerClockOwnerBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerClockManager.class)
public abstract class ServerClockManagerMixin_NeoForge {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel arclight$worldTickGuard(MinecraftServer server) {
        ServerLevel owner = ((ServerClockOwnerBridge) this).arclight$owner();
        return owner == null ? server.overworld() : owner;
    }
}
