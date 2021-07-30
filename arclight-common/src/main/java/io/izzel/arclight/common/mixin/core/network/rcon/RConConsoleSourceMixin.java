package io.izzel.arclight.common.mixin.core.network.rcon;

import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.network.rcon.RConConsoleSourceBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.rcon.RconConsoleSource;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RconConsoleSource.class)
public class RConConsoleSourceMixin implements ICommandSourceBridge, RConConsoleSourceBridge {

    // @formatter:off
    @Shadow @Final private StringBuffer buffer;
    @Shadow @Final private MinecraftServer server;
    // @formatter:on

    public CommandSender getBukkitSender() {
        return ((MinecraftServerBridge) this.server).bridge$getRemoteConsole();
    }

    public void sendMessage(String message) {
        this.buffer.append(message);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender();
    }

    @Override
    public void bridge$sendMessage(String message) {
        sendMessage(message);
    }
}
