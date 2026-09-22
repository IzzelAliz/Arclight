package io.izzel.arclight.common.mixin.core.server.rcon;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.server.rcon.RconConsoleSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.rcon.RconConsoleSource;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftRemoteConsoleCommandSender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RconConsoleSource.class)
public class RconConsoleSourceMixin implements CommandSourceBridge, RconConsoleSourceBridge {

    // @formatter:off
    @Shadow @Final private StringBuffer buffer;
    // @formatter:on

    public java.net.SocketAddress socketAddress;

    @Override
    public void bridge$setSocketAddress(java.net.SocketAddress address) {
        this.socketAddress = address;
    }

    @Unique
    private CraftRemoteConsoleCommandSender arclight$remoteConsole;

    public CommandSender getBukkitSender() {
        if (this.arclight$remoteConsole == null) {
            this.arclight$remoteConsole = new CraftRemoteConsoleCommandSender((RconConsoleSource) (Object) this);
        }
        return this.arclight$remoteConsole;
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
