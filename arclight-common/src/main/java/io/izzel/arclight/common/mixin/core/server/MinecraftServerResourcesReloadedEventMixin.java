package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.CommandResourceReloadBridge;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerResourcesReloadedEventMixin implements CommandResourceReloadBridge {

    @Override
    public CompletableFuture<Void> bridge$notifyCommandResourceReload(CompletableFuture<Void> reload) {
        // Only ReloadCommand.reloadPacks opts in. CraftServer.reloadData and
        // PaperDatapack.setEnabled already notify PLUGIN on their own success stage.
        // Waiting for the returned future also includes the native/loader finalizers
        // and the command-lifecycle continuation, not merely resource adoption.
        return reload.thenRunAsync(() -> {
            CraftBlockData.reloadCache();
            new ServerResourcesReloadedEvent(ServerResourcesReloadedEvent.Cause.COMMAND).callEvent();
        }, (MinecraftServer) (Object) this);
    }
}
