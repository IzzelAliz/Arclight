package io.izzel.arclight.common.mod.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.BukkitCommandWrapper;
import org.bukkit.craftbukkit.v.command.VanillaCommandWrapper;

public class BukkitDispatcher extends CommandDispatcher<CommandSource> {

    private final Commands commands;

    public BukkitDispatcher(Commands commands) {
        this.commands = commands;
    }

    @Override
    public LiteralCommandNode<CommandSource> register(LiteralArgumentBuilder<CommandSource> command) {
        LiteralCommandNode<CommandSource> node = command.build();
        if (!(node.getCommand() instanceof BukkitCommandWrapper)) {
            VanillaCommandWrapper wrapper = new VanillaCommandWrapper(this.commands, node);
            ((CraftServer) Bukkit.getServer()).getCommandMap().register("forge", wrapper);
        }
        getRoot().addChild(node);
        return node;
    }
}
