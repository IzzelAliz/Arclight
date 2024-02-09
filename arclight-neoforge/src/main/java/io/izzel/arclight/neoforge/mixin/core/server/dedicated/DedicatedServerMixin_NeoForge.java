package io.izzel.arclight.neoforge.mixin.core.server.dedicated;

import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.spongepowered.asm.mixin.Mixin;

import java.io.IOException;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin_NeoForge implements DedicatedServerBridge {

    @Override
    public void bridge$platform$exitNow() {
        try {
            TerminalConsoleAppender.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
