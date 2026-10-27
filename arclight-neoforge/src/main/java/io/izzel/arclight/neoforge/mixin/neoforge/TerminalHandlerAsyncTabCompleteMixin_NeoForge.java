package io.izzel.arclight.neoforge.mixin.neoforge;

import io.izzel.arclight.common.mod.util.ArclightTerminalReader;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.neoforge.server.console.TerminalHandler;
import org.jline.reader.Completer;
import org.jline.reader.LineReaderBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TerminalHandler.class, remap = false)
public abstract class TerminalHandlerAsyncTabCompleteMixin_NeoForge {

    @Decorate(method = "handleCommands(Lnet/minecraft/server/dedicated/DedicatedServer;)Z",
        at = @At(value = "INVOKE", target = "Lorg/jline/reader/LineReaderBuilder;completer(Lorg/jline/reader/Completer;)Lorg/jline/reader/LineReaderBuilder;", remap = false),
        require = 1, allow = 1)
    private static LineReaderBuilder arclight$asyncCompleter(LineReaderBuilder builder, Completer original,
                                                            @Local(ordinal = 0) DedicatedServer server) throws Throwable {
        // Keep NeoForge's terminal lifecycle and builder; share the event-aware
        // completer rather than calling its native provider a second time.
        return (LineReaderBuilder) DecorationOps.callsite().invoke(builder, ArclightTerminalReader.createCommandCompleter(server));
    }
}
