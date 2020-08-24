package io.izzel.arclight.common.mixin.core.entity.projectile;

import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractFireballEntity.class)
public abstract class AbstractFireballEntityMixin extends DamagingProjectileEntityMixin {

    @Inject(method = "readAdditional", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/AbstractFireballEntity;setStack(Lnet/minecraft/item/ItemStack;)V"))
    private void arclight$nonNullItem(CompoundNBT compound, CallbackInfo ci, ItemStack stack) {
        if (stack.isEmpty()) ci.cancel();
    }
}
