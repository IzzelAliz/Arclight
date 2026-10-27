package io.izzel.arclight.common.mixin.core.network.chat;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.adventure.AdventureComponent;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MutableComponent.class)
public abstract class MutableComponentMixin {

    @Decorate(method = "equals", inject = true, at = @At("HEAD"))
    private void arclight$compareAdventureComponent(Object other) throws Throwable {
        if (other instanceof AdventureComponent component) {
            DecorationOps.cancel().invoke(this.equals(component.deepConverted()));
            return;
        }
    }
}
