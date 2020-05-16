package io.izzel.arclight.mixin.core.entity.item.minecart;

import io.izzel.arclight.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.bridge.entity.EntityBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.item.minecart.MinecartCommandBlockEntity;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecartCommandBlockEntity.MinecartCommandLogic.class)
public abstract class MinecartCommandBlockEntity_MinecartCommandLogicMixin implements ICommandSourceBridge {

    @Shadow(aliases = {"this$0", "field_210168_a"}) private MinecartCommandBlockEntity outerThis;

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return ((EntityBridge) outerThis).bridge$getBukkitEntity();
    }
}
