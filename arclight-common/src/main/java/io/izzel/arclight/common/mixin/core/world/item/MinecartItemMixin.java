package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartItem.class)
public class MinecartItemMixin {

    @Eject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$entityPlace(Level world, Entity entityIn, CallbackInfoReturnable<InteractionResult> cir, UseOnContext context) {
        if (DistValidate.isValid(world) && CraftEventFactory.callEntityPlaceEvent(context, entityIn).isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return false;
        } else if (!world.addFreshEntity(entityIn)) {
            cir.setReturnValue(InteractionResult.PASS);
            return false;
        } else {
            return true;
        }
    }
}
