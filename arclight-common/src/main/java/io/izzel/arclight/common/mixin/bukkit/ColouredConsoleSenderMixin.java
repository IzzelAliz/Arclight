package io.izzel.arclight.common.mixin.bukkit;

import jline.Terminal;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ColouredConsoleSender.class, remap = false)
public class ColouredConsoleSenderMixin extends CraftConsoleCommandSenderMixin {

    private static final Logger LOGGER = LogManager.getLogger("Console");

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljline/console/ConsoleReader;getTerminal()Ljline/Terminal;"))
    private Terminal arclight$terminal(ConsoleReader instance) {
        return null;
    }

    /**
     * @author IzzelAliz
     * @reason use TerminalConsoleAppender
     */
    @Overwrite
    public void sendMessage(String message) {
        if (!this.conversationTracker.isConversingModaly()) {
            LOGGER.info(message);
        }
    }
}
