package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.EntityTypeBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(value = CraftWorld.class, remap = false)
public class CraftWorldMixin {

    @Inject(method = "spawnEntity", cancellable = true, at = @At("HEAD"))
    private void arclight$useFactory(Location loc, EntityType entityType, CallbackInfoReturnable<Entity> cir) {
        Function<Location, ? extends net.minecraft.entity.Entity> factory = ((EntityTypeBridge) (Object) entityType).bridge$entityFactory();
        if (factory != null) {
            cir.setReturnValue(((EntityBridge) factory.apply(loc)).bridge$getBukkitEntity());
        }
    }
}
