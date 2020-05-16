package io.izzel.arclight.mixin.core.entity.monster;

import io.izzel.arclight.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PillagerEntity.class)
public abstract class PillagerEntityMixin extends CreatureEntityMixin {

    // @formatter:off
    @Shadow public abstract Inventory getInventory();
    // @formatter:on

    @Inject(method = "canDespawn", cancellable = true, at = @At("HEAD"))
    private void arclight$nullInventory(double distanceToClosestPlayer, CallbackInfoReturnable<Boolean> cir) {
        if (this.getInventory() == null) cir.setReturnValue(false);
    }
}
