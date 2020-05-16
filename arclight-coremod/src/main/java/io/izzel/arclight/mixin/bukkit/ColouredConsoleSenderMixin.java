package io.izzel.arclight.mixin.bukkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v1_14_R1.command.ColouredConsoleSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.PrintStream;

@Mixin(ColouredConsoleSender.class)
public class ColouredConsoleSenderMixin {

    private static final Logger LOGGER = LogManager.getLogger("Console");

    @Redirect(method = "sendMessage", remap = false, at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/String;)V"))
    public void arclight$sendMessage(PrintStream printStream, String x) {
        LOGGER.info(x);
    }
}
