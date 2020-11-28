package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.world.server.ServerChunkProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.server.ServerChunkProvider$ChunkExecutor")
public abstract class ServerChunkProvider_ChunkExecutorMixin extends ThreadTaskExecutor<Runnable> {

    // @formatter:off
    @Shadow(aliases = {"this$0", "field_213181_a"}, remap = false) @Final private ServerChunkProvider outer;
    // @formatter:on

    protected ServerChunkProvider_ChunkExecutorMixin(String nameIn) {
        super(nameIn);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected boolean driveOne() {
        try {
            if (((ServerChunkProviderBridge) outer).bridge$tickDistanceManager()) {
                return true;
            } else {
                ((ServerChunkProviderBridge) outer).bridge$getLightManager().func_215588_z_();
                return super.driveOne();
            }
        } finally {
            ((ChunkManagerBridge) outer.chunkManager).bridge$getCallbackExecutor().run();
            ((MinecraftServerBridge) ArclightServer.getMinecraftServer()).bridge$drainQueuedTasks();
        }
    }
}
