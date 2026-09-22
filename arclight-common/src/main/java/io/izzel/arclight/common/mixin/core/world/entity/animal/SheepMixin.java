package io.izzel.arclight.common.mixin.core.world.entity.animal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.animal.sheep.Sheep.class)
public abstract class SheepMixin extends AnimalMixin {

    //Force drop handler moved to PSI

    @Inject(method = "ate", cancellable = true, at = @At("HEAD"))
    private void arclight$regrow(CallbackInfo ci) {
        SheepRegrowWoolEvent event = new SheepRegrowWoolEvent((Sheep) this.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

}
