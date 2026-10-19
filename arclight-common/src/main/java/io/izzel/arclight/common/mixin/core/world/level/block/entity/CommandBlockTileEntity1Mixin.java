package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.command.CraftBlockCommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/level/block/entity/CommandBlockEntity$1")
public class CommandBlockTileEntity1Mixin implements CommandSourceBridge {
    @Shadow(aliases = {"this$0", "f_59153_", "field_11921"}, remap = false)
    private CommandBlockEntity outerThis;

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return new CraftBlockCommandSender(wrapper, outerThis);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }
}