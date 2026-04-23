package io.izzel.arclight.forge.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerLoginPacketListenerImplBridge;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin_Forge implements ServerLoginPacketListenerImplBridge {

    @Override
    public Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(SidedThreadGroups.SERVER, runnable, name);
    }
}
