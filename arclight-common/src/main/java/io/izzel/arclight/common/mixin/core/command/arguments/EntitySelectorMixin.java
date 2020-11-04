package io.izzel.arclight.common.mixin.core.command.arguments;

import io.izzel.arclight.common.bridge.command.CommandSourceBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Redirect(method = "checkPermission", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;hasPermissionLevel(I)Z"))
    private boolean arclight$stringPermission(CommandSource commandSource, int level) {
        return ((CommandSourceBridge) commandSource).bridge$hasPermission(level, "minecraft.command.selector");
    }
}
