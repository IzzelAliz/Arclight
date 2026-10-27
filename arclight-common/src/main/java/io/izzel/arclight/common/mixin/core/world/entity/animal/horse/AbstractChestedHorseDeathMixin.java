package io.izzel.arclight.common.mixin.core.world.entity.animal.horse;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractChestedHorse.class)
public class AbstractChestedHorseDeathMixin {
    @WrapOperation(method = "dropEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/AbstractChestedHorse;setChest(Z)V"))
    private void arclight$deferChest(AbstractChestedHorse horse, boolean value, Operation<Void> original) {
        ((DeathEventBridge) horse).bridge$deferDeathChange(() -> original.call(horse, value));
    }
}
