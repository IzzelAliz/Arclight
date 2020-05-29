package io.izzel.arclight.mixin.bukkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v1_14_R1.command.CraftConsoleCommandSender;
import org.bukkit.craftbukkit.v1_14_R1.conversations.ConversationTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftConsoleCommandSender.class, remap = false)
public class CraftConsoleCommandSenderMixin {

    private static final Logger LOGGER = LogManager.getLogger("Console");

    @Shadow @Final protected ConversationTracker conversationTracker;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void sendRawMessage(String message) {
        LOGGER.info(message);
    }

}
