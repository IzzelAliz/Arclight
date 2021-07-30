package io.izzel.arclight.common.mixin.core.commands.arguments.selector;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Redirect(method = "checkPermissions", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;hasPermission(I)Z"))
    private boolean arclight$stringPermission(CommandSourceStack commandSource, int level) {
        return ((CommandSourceBridge) commandSource).bridge$hasPermission(level, "minecraft.command.selector");
    }
}
