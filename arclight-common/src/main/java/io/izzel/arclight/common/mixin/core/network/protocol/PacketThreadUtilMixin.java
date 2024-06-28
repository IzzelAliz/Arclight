package io.izzel.arclight.common.mixin.core.network.protocol;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketUtils.class)
public abstract class PacketThreadUtilMixin {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow public static <T extends PacketListener> ReportedException makeReportedException(Exception exception, Packet<T> packet, T packetListener) { throw new RuntimeException(); }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> packetIn, T processor, BlockableEventLoop<?> executor) throws RunningOnDifferentThreadException {
        if (!executor.isSameThread()) {
            executor.executeIfPossible(() -> {
                if (processor instanceof ServerCommonPacketListenerImpl && ((ServerCommonPacketListenerBridge) processor).bridge$processedDisconnect()) {
                    return;
                }
                if (processor.isAcceptingMessages()) {
                    try {
                        packetIn.handle(processor);
                    } catch (Exception exception) {
                        if (exception instanceof ReportedException reportedexception) {
                            if (reportedexception.getCause() instanceof OutOfMemoryError) {
                                throw makeReportedException(exception, packetIn, processor);
                            }
                        }
                        processor.onPacketError(packetIn, exception);
                    }
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: {}", packetIn);
                }

            });
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }
}
