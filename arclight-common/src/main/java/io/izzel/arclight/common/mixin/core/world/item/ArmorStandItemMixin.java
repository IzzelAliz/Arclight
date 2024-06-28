package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    private transient ArmorStand arclight$entity;

    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;moveTo(DDDFF)V"))
    public void arclight$captureEntity(ArmorStand armorStandEntity, double x, double y, double z, float yaw, float pitch) {
        armorStandEntity.moveTo(x, y, z, yaw, pitch);
        arclight$entity = armorStandEntity;
    }

    @Decorate(method = "useOn", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    public void arclight$entityPlace(UseOnContext context, @Local(ordinal = -1) ArmorStand armorStand) throws Throwable {
        if (DistValidate.isValid(context) && CraftEventFactory.callEntityPlaceEvent(context, armorStand).isCancelled()) {
            DecorationOps.cancel().invoke(InteractionResult.FAIL);
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
