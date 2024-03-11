package io.izzel.arclight.common.mixin.core.network.protocol;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;
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
                if (processor instanceof ServerCommonPacketListenerBridge && ((ServerCommonPacketListenerBridge) processor).bridge$processedDisconnect()) {
                    return;
                }
                if (processor.isAcceptingMessages()) {
                    try {
                        packetIn.handle(processor);
                    } catch (Exception exception) {
                        if (exception instanceof ReportedException reportedexception) {
                            if (reportedexception.getCause() instanceof OutOfMemoryError) {
                                throw exception;
                            }
                        }
                        if (processor.shouldPropagateHandlingExceptions()) {
                            if (exception instanceof ReportedException r) {
                                processor.fillCrashReport(r.getReport());
                                throw exception;
                            } else {
                                CrashReport crashreport = CrashReport.forThrowable(exception, "Main thread packet handler");
                                processor.fillCrashReport(crashreport);
                                throw new ReportedException(crashreport);
                            }
                        }
                        LOGGER.error("Failed to handle packet {}, suppressing error", packetIn, exception);
                    }
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: " + packetIn);
                }

            });
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }
}
