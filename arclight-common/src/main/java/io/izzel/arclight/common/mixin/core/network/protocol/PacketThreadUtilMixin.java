package io.izzel.arclight.common.mixin.core.network.protocol;

import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketUtils.class)
public class PacketThreadUtilMixin {

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> packetIn, T processor, BlockableEventLoop<?> executor) throws RunningOnDifferentThreadException {
        if (!executor.isSameThread()) {
            executor.executeIfPossible(() -> {
                if (((MinecraftServerBridge) ((CraftServer) Bukkit.getServer()).getServer()).bridge$hasStopped() || (processor instanceof ServerGamePacketListenerImpl && ((ServerPlayNetHandlerBridge) processor).bridge$processedDisconnect())) {
                    return;
                }
                if (processor.isAcceptingMessages()) {
                    try {
                        packetIn.handle(processor);
                    } catch (Exception exception) {
                        if (processor.shouldPropagateHandlingExceptions()) {
                            throw exception;
                        }
                        LOGGER.error("Failed to handle packet {}, suppressing error", packetIn, exception);
                    }
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: " + packetIn);
                }

            });
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        } else if (((MinecraftServerBridge) ((CraftServer) Bukkit.getServer()).getServer()).bridge$hasStopped() || (processor instanceof ServerGamePacketListenerImpl && ((ServerPlayNetHandlerBridge) processor).bridge$processedDisconnect())) {
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }
}
