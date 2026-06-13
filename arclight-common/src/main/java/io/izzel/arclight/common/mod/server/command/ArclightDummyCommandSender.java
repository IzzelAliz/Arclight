package io.izzel.arclight.common.mod.server.command;

import io.izzel.arclight.common.mod.server.permission.ArclightDummyPermissible;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ArclightDummyCommandSender extends ArclightDummyPermissible implements CommandSender {

    public CommandSourceStack stack;
    public Spigot spigot;

    public ArclightDummyCommandSender(CommandSourceStack stack) {
        this.stack = stack;
    }

    public class Spigot extends CommandSender.Spigot {
        @Override
        public void sendMessage(@NotNull BaseComponent... components) {
            for (var raw: components) {
                sendMessage(raw);
            }
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull BaseComponent component) {
            sendMessage(component);
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull BaseComponent... components) {
            sendMessage(components);
        }

        @Override
        public void sendMessage(@NotNull BaseComponent component) {
            var result = CraftChatMessage.fromJSON(ComponentSerializer.toString(component));
            if (result != null) {
                stack.sendSystemMessage(result);
            }
        }
    }

    @Override
    public void sendMessage(@NotNull String s) {
        for (var msg: CraftChatMessage.fromString(s)) {
            stack.sendSystemMessage(msg);
        }
    }

    @Override
    public void sendMessage(@NotNull String... strings) {
        for (var raw: strings) {
            sendMessage(raw);
        }
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {
        sendMessage(s);
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String... strings) {
        sendMessage(strings);
    }

    @Override
    public @NotNull Server getServer() {
        // TODO: Use stack.getServer().bridge$getServer() after PR #1724 is merged
        return Bukkit.getServer();
    }

    @Override
    public @NotNull String getName() {
        return stack.getTextName();
    }

    @Override
    public @NotNull Spigot spigot() {
        if (spigot != null) {
            return spigot;
        }
        return (spigot = new Spigot());
    }
}
