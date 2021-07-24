package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$MainThreadExecutor")
public abstract class ServerChunkProvider_ChunkExecutorMixin extends BlockableEventLoop<Runnable> {

    // @formatter:off
    @Shadow(aliases = {"this$0", "field_213181_a"}, remap = false) @Final private ServerChunkCache outer;
    // @formatter:on

    protected ServerChunkProvider_ChunkExecutorMixin(String nameIn) {
        super(nameIn);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected boolean pollTask() {
        try {
            if (((ServerChunkProviderBridge) outer).bridge$tickDistanceManager()) {
                return true;
            } else {
                ((ServerChunkProviderBridge) outer).bridge$getLightManager().tryScheduleUpdate();
                return super.pollTask();
            }
        } finally {
            ((ChunkManagerBridge) outer.chunkMap).bridge$getCallbackExecutor().run();
            ((MinecraftServerBridge) ArclightServer.getMinecraftServer()).bridge$drainQueuedTasks();
        }
    }
}
