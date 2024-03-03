package io.izzel.arclight.common.mixin.vanilla.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.vanilla.world.entity.EntityMixin_Vanilla;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin_Vanilla extends EntityMixin_Vanilla {

    @Redirect(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/MushroomCow;discard()V"))
    private void arclight$animalTransformPre(MushroomCow mushroomCow) {
    }

    @Inject(method = "shear", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$animalTransform(SoundSource source, CallbackInfo ci, Cow cowEntity) {
        if (CraftEventFactory.callEntityTransformEvent((MushroomCow) (Object) this, cowEntity, EntityTransformEvent.TransformReason.SHEARED).isCancelled()) {
            ci.cancel();
        } else {
            ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
            this.discard();
        }
    }

    @Redirect(method = "shear", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$shearDrop(Level instance, Entity entity) {
        var itemEntity = (ItemEntity) entity;
        EntityDropItemEvent event = new EntityDropItemEvent(this.bridge$getBukkitEntity(), (org.bukkit.entity.Item) itemEntity.bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return instance.addFreshEntity(entity);
    }
}
