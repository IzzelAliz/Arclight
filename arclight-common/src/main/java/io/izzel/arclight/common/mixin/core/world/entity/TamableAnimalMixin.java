package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityToggleSitEvent;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin extends AnimalMixin {

    // @formatter:off
    @Shadow public abstract boolean isTame();

    @org.spongepowered.asm.mixin.injection.Inject(method = "feed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;heal(F)V"))
    private void arclight$foodHeal(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.bridge$pushHealReason(org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.EATING);
    }

    // @formatter:on

    @Unique private boolean arclight$skipSitEvent;

    @Decorate(method = "setInSittingPose(Z)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$toggleSit(boolean value) throws Throwable {
        // NBT restoration bypasses only this setter entry, not later nested setters.
        boolean callEvent = !this.arclight$skipSitEvent;
        this.arclight$skipSitEvent = false;
        if (callEvent && !new EntityToggleSitEvent(this.getBukkitEntity(), value).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Decorate(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;setInSittingPose(Z)V"), require = 1, allow = 1)
    private void arclight$restoreSittingPose(TamableAnimal animal, boolean value) throws Throwable {
        boolean previous = this.arclight$skipSitEvent;
        this.arclight$skipSitEvent = true;
        try {
            DecorationOps.callsite().invoke(animal, value);
        } finally {
            this.arclight$skipSitEvent = previous;
        }
    }

    @Decorate(method = "maybeTeleportTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;snapTo(DDDFF)V"))
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
