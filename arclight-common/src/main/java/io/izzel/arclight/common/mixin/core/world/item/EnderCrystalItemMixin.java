package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystalItem.class)
public class EnderCrystalItemMixin {

    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;setShowBottom(Z)V"))
    public void arclight$captureEntity(EndCrystal enderCrystalEntity, boolean showBottom) {
        arclight$enderCrystalEntity = enderCrystalEntity;
        enderCrystalEntity.setShowBottom(showBottom);
    }

    private transient EndCrystal arclight$enderCrystalEntity;

    @Inject(method = "useOn", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    public void arclight$entityPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (DistValidate.isValid(context) && CraftEventFactory.callEntityPlaceEvent(context, arclight$enderCrystalEntity).isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
        arclight$enderCrystalEntity = null;
    }
}
