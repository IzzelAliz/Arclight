package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(CatEntity.class)
public abstract class CatEntityMixin extends AnimalEntityMixin {

    @Redirect(method = "processInteract", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Random;nextInt(I)I"))
    private int arclight$catTame(Random random, int bound, PlayerEntity playerEntity) {
        return random.nextInt(bound) == 0 && !CraftEventFactory.callEntityTameEvent((CatEntity) (Object) this, playerEntity).isCancelled() ? 0 : 2;
    }
}
