package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/level/block/entity/CommandBlockEntity$1")
public class CommandBlockTileEntity1Mixin implements ICommandSourceBridge {

    @Shadow(aliases = {"this$0", "field_145767_a"}, remap = false) private CommandBlockEntity outerThis;

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return new CraftBlockCommandSender(wrapper, outerThis);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }
}
