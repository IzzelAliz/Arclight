package io.izzel.arclight.common.mixin.bukkit;

import com.mojang.brigadier.context.CommandContext;
import org.bukkit.craftbukkit.v.command.BukkitCommandWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BukkitCommandWrapper.class)
public class BukkitCommandWrapperMixin {

    @Redirect(method = "run", remap = false, at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/context/CommandContext;getInput()Ljava/lang/String;"))
    public String arclight$removeSlash(CommandContext<?> context) {
        String input = context.getInput();
        if (input.startsWith("/")) return input.substring(1);
        return input;
    }
}
