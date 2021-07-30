package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$MainThreadExecutor")
public abstract class ServerChunkCache_MainThreadExecutorMixin extends BlockableEventLoop<Runnable> {

    // @formatter:off
    @Shadow(aliases = {"this$0", "f_8491_"}, remap = false) @Final private ServerChunkCache outer;
    // @formatter:on

    protected ServerChunkCache_MainThreadExecutorMixin(String nameIn) {
        super(nameIn);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean pollTask() {
        try {
            if (((ServerChunkProviderBridge) outer).bridge$tickDistanceManager()) {
                return true;
            } else {
                ((ServerChunkProviderBridge) outer).bridge$getLightManager().tryScheduleUpdate();
                return super.pollTask();
            }
        } finally {
            ((ChunkMapBridge) outer.chunkMap).bridge$getCallbackExecutor().run();
            ((MinecraftServerBridge) ArclightServer.getMinecraftServer()).bridge$drainQueuedTasks();
        }
    }
}
