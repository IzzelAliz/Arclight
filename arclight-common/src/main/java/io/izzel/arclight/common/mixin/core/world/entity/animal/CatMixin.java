package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// how can we not having cats before
@Mixin(Cat.class)
public abstract class CatMixin extends AnimalMixin {

    @Redirect(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) {
        var ret = instance.nextInt(i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }
}
