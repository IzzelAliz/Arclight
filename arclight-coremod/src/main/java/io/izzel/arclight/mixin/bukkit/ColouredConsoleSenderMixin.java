package io.izzel.arclight.mixin.bukkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v1_14_R1.command.ColouredConsoleSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ColouredConsoleSender.class, remap = false)
public class ColouredConsoleSenderMixin extends CraftConsoleCommandSenderMixin {

    private static final Logger LOGGER = LogManager.getLogger("Console");

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
