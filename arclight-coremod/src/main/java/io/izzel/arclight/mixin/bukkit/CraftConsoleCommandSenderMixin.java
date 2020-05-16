package io.izzel.arclight.mixin.bukkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_14_R1.command.CraftConsoleCommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CraftConsoleCommandSender.class)
public class CraftConsoleCommandSenderMixin {

    private static final Logger LOGGER = LogManager.getLogger("Console");

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public void sendRawMessage(String message) {
        LOGGER.info(ChatColor.stripColor(message));
    }

}
