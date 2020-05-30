package io.izzel.arclight.common.mixin.core.network.rcon;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.network.rcon.RConConsoleSourceBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RConConsoleSource.class)
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
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return getBukkitSender();
    }

    @Override
    public void bridge$sendMessage(String message) {
        sendMessage(message);
    }
}
