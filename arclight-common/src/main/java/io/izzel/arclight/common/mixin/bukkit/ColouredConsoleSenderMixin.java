package io.izzel.arclight.common.mixin.bukkit;

import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;
import org.jline.terminal.Terminal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ColouredConsoleSender.class, remap = false)
public class ColouredConsoleSenderMixin extends CraftConsoleCommandSenderMixin {

    private static final Logger LOGGER = LogManager.getLogger("Console");

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/v1_21_R7/CraftServer;getTerminal()Lorg/jline/terminal/Terminal;"))
    private Terminal arclight$terminal(CraftServer instance) {
        return TerminalConsoleAppender.getTerminal();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/jline/terminal/Terminal;getType()Ljava/lang/String;"))
    private String arclight$terminalType(Terminal instance) {
        return instance == null ? Terminal.TYPE_DUMB : instance.getType();
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
