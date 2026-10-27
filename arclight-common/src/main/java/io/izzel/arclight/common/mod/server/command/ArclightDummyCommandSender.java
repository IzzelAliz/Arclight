package io.izzel.arclight.common.mod.server.command;

import io.izzel.arclight.common.mod.server.permission.ArclightDummyPermissible;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.commands.CommandSourceStack;

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
            stack.sendSystemMessage(CraftChatMessage.bungeeToVanilla(components));
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
            stack.sendSystemMessage(CraftChatMessage.bungeeToVanilla(component));
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
    public net.kyori.adventure.text.@NotNull Component name() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(stack.getDisplayName());
    }

    @Override
    public void sendMessage(net.kyori.adventure.identity.Identity identity, net.kyori.adventure.text.Component message, net.kyori.adventure.audience.MessageType type) {
        stack.sendSystemMessage(io.papermc.paper.adventure.PaperAdventure.asVanilla(message));
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
