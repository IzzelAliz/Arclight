package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.tileentity.CommandBlockTileEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/tileentity/CommandBlockTileEntity$1")
public class CommandBlockTileEntity1Mixin implements ICommandSourceBridge {

    @Shadow(aliases = {"this$0", "field_145767_a"}, remap = false) private CommandBlockTileEntity outerThis;

    public CommandSender getBukkitSender(CommandSource wrapper) {
        return new CraftBlockCommandSender(wrapper, outerThis);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return getBukkitSender(wrapper);
    }
}
