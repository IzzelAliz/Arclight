package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Fireball.class)
public abstract class FireballMixin extends AbstractHurtingProjectileMixin {

    @Inject(method = "readAdditionalSaveData", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Fireball;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$nonNullItem(CompoundTag compound, CallbackInfo ci, ItemStack stack) {
        if (stack.isEmpty()) ci.cancel();
    }
}
