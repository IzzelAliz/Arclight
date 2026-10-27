package io.izzel.arclight.common.mixin.core.commands.arguments.selector;

import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {

    @Decorate(method = "checkPermissions(Lnet/minecraft/commands/CommandSourceStack;)V", inject = true, at = @At("HEAD"))
    private void arclight$bypassSelectorPermissions(CommandSourceStack sender) throws Throwable {
        if (((CommandSourceStackBridge) sender).bridge$isBypassSelectorPermissions()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
