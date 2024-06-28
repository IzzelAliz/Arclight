package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TamableAnimal.class)
public abstract class TameableAnimalMixin extends AnimalMixin {

    // @formatter:off
    @Shadow public abstract boolean isTame();
    // @formatter:on

    @Decorate(method = "maybeTeleportTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;moveTo(DDDFF)V"))
    private void arclight$teleportEvent(TamableAnimal instance, double x, double y, double z, float yaw, float pitch) throws Throwable {
        EntityTeleportEvent event = CraftEventFactory.callEntityTeleportEvent(instance, x, y, z);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
        Location to = event.getTo();
        DecorationOps.callsite().invoke(instance, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
    }
}
