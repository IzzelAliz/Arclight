package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketThreadUtil.class)
public class PacketThreadUtilMixin {

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <T extends INetHandler> void checkThreadAndEnqueue(IPacket<T> packetIn, T processor, ThreadTaskExecutor<?> executor) throws ThreadQuickExitException {
        if (!executor.isOnExecutionThread()) {
            executor.execute(() -> {
                if (((MinecraftServerBridge) ((CraftServer) Bukkit.getServer()).getServer()).bridge$hasStopped() || (processor instanceof ServerPlayNetHandler && ((ServerPlayNetHandlerBridge) processor).bridge$processedDisconnect())) {
                    return;
                }
                if (processor.getNetworkManager().isChannelOpen()) {
                    packetIn.processPacket(processor);
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: " + packetIn);
                }

            });
            throw ThreadQuickExitException.INSTANCE;
        } else if (((MinecraftServerBridge) ((CraftServer) Bukkit.getServer()).getServer()).bridge$hasStopped() || (processor instanceof ServerPlayNetHandler && ((ServerPlayNetHandlerBridge) processor).bridge$processedDisconnect())) {
            throw ThreadQuickExitException.INSTANCE;
        }
    }
}
