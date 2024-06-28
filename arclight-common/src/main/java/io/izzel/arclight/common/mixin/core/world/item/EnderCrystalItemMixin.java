package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalItem.class)
public class EnderCrystalItemMixin {

    @Decorate(method = "useOn", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$entityPlace(UseOnContext context, @Local(ordinal = -1) EndCrystal enderCrystalEntity) throws Throwable {
        if (DistValidate.isValid(context) && CraftEventFactory.callEntityPlaceEvent(context, enderCrystalEntity).isCancelled()) {
            DecorationOps.cancel().invoke(InteractionResult.FAIL);
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
