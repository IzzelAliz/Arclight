package io.izzel.arclight.common.mod.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import io.papermc.paper.command.brigadier.bukkit.BukkitCommandNode;
import org.bukkit.craftbukkit.command.VanillaCommandWrapper;

public class BukkitDispatcher extends CommandDispatcher<CommandSourceStack> {

    private final Commands commands;

    public BukkitDispatcher(Commands commands) {
        this.commands = commands;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> register(LiteralArgumentBuilder<CommandSourceStack> command) {
        LiteralCommandNode<CommandSourceStack> node = command.build();
        if (!((Object) node.getCommand() instanceof BukkitCommandNode.BukkitBrigCommand)) {
            VanillaCommandWrapper wrapper = new VanillaCommandWrapper(node);
            ((CraftServer) Bukkit.getServer()).getCommandMap().register("forge", wrapper);
        }
        getRoot().addChild(node);
        return node;
    }
}
