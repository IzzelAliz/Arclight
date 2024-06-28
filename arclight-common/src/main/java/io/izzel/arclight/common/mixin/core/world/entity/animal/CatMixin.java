package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Cat.class)
public abstract class CatMixin extends AnimalMixin {

    @Decorate(method = "tryToTame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) throws Throwable {
        var ret = (int) DecorationOps.callsite().invoke(instance, i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }
}
