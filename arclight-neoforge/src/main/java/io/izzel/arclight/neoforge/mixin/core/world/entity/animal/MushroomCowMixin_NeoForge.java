package io.izzel.arclight.neoforge.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.neoforge.mixin.core.world.entity.MobMixin_NeoForge;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
public abstract class MushroomCowMixin_NeoForge extends MobMixin_NeoForge {

    // @formatter:off
    // @formatter:on

    @Redirect(method = "shear", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/entity/animal/MushroomCow;discard()V"))
    private void arclight$animalTransformPre(MushroomCow mushroomCow) {
    }

    @Inject(method = "shear", remap = false, cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$animalTransform(SoundSource p_28924_, CallbackInfo ci, Cow cowEntity) {
        if (CraftEventFactory.callEntityTransformEvent((MushroomCow) (Object) this, cowEntity, EntityTransformEvent.TransformReason.SHEARED).isCancelled()) {
            ci.cancel();
        } else {
            ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
            this.discard();
        }
    }

    @Redirect(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/MushroomCow;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity arclight$onShearDrop(MushroomCow cow, ItemStack stack, float v) {
        var itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(1.0D), this.getZ(), stack);
        EntityDropItemEvent event = new EntityDropItemEvent(this.bridge$getBukkitEntity(), (org.bukkit.entity.Item) itemEntity.bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        this.level().addFreshEntity(itemEntity);
        return itemEntity;
    }
}
