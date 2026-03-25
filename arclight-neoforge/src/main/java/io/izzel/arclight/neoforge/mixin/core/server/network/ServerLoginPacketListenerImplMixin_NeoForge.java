package io.izzel.arclight.neoforge.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerLoginPacketListenerImplBridge;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin_NeoForge implements ServerLoginPacketListenerImplBridge {

    @Override
    public Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(SidedThreadGroups.SERVER, runnable, name);
    }
}
