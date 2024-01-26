package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Ocelot.class)
public abstract class OcelotMixin extends AnimalMixin {

    // @formatter:off
    @Shadow abstract boolean isTrusting();
    // @formatter:on

    public boolean spawnBonus = true;

    @Redirect(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) {
        var ret = instance.nextInt(i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }
}
