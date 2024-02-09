package io.izzel.arclight.neoforge.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_NeoForge implements MinecraftServerBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract void markWorldsDirty();
    // @formatter:on

    @Override
    public void bridge$platform$loadLevel(Level level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }

    @Override
    public void bridge$platform$unloadLevel(Level level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
    }

    @Override
    public void bridge$forge$markLevelsDirty() {
        this.markWorldsDirty();
    }

    @Override
    public void bridge$platform$serverStarted() {
        ServerLifecycleHooks.handleServerStarted((MinecraftServer) (Object) this);
    }

    @Override
    public void bridge$platform$serverStopping() {
        ServerLifecycleHooks.handleServerStopping((MinecraftServer) (Object) this);
    }

    @Override
    public void bridge$forge$expectServerStopped() {
        ServerLifecycleHooks.expectServerStopped();
    }

    @Override
    public void bridge$platform$serverStopped() {
        ServerLifecycleHooks.handleServerStopped((MinecraftServer) (Object) this);
    }

    @Override
    public void bridge$forge$reinstatePersistentChunks(ServerLevel level, ForcedChunksSavedData savedData) {
        ForcedChunkManager.reinstatePersistentChunks(level, savedData);
    }
}
