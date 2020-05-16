package io.izzel.arclight.mixin.core.tileentity;

import net.minecraft.tileentity.CommandBlockLogic;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockLogic.class)
public class CommandBlockLogicMixin {

    // @formatter:off
    @Shadow private ITextComponent customName;
    // @formatter:on

    @Inject(method = "setName", cancellable = true, at = @At("HEAD"))
    public void arclight$setName(ITextComponent nameIn, CallbackInfo ci) {
        if (nameIn == null) {
            this.customName = new StringTextComponent("@");
            ci.cancel();
        }
    }
}
