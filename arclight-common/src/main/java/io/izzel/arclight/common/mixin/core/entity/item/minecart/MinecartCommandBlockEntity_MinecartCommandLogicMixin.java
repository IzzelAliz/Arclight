package io.izzel.arclight.common.mixin.core.entity.item.minecart;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.item.minecart.MinecartCommandBlockEntity;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecartCommandBlockEntity.MinecartCommandLogic.class)
public abstract class MinecartCommandBlockEntity_MinecartCommandLogicMixin implements ICommandSourceBridge {

    @Shadow(aliases = {"this$0", "field_210168_a"}, remap = false) private MinecartCommandBlockEntity outerThis;

    public CommandSender getBukkitSender(CommandSource wrapper) {
        return ((EntityBridge) outerThis).bridge$getBukkitEntity();
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return getBukkitSender(wrapper);
    }
}
