package io.izzel.arclight.forge.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin_Forge implements ServerLoginNetHandlerBridge {

    @Override
    public Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(SidedThreadGroups.SERVER, runnable, name);
    }

    @Override
    public FriendlyByteBuf bridge$getDiscardedQueryAnswerData(ServerboundCustomQueryAnswerPacket payload) {
        return payload.getInternalData();
    }
}
