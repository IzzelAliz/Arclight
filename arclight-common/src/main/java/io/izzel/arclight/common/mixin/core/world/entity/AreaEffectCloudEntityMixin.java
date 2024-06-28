package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.AreaEffectCloudEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudEntityMixin extends EntityMixin implements AreaEffectCloudEntityBridge {

    // @formatter:off
    @Shadow @Final private Map<Entity, Integer> victims;
    @Shadow @Nullable public abstract LivingEntity getOwner();
    // @formatter:on

    @SuppressWarnings("unchecked")
    @Decorate(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<LivingEntity> arclight$effectApply(Level instance, Class<LivingEntity> cl, AABB aabb,
                                                    @Local(ordinal = 0) float radius,
                                                    @Local(ordinal = -1) List<MobEffectInstance> effects) throws Throwable {
        var entities = (List<LivingEntity>) DecorationOps.callsite().invoke(instance, cl, aabb);
        var affected = entities.stream().filter(it -> !this.victims.containsKey(it) && it.isAffectedByPotions())
            .filter(it -> effects.stream().anyMatch(it::canBeAffected))
            .filter(it -> {
                double d3 = it.getX() - this.getX();
                double d4 = it.getZ() - this.getZ();
                double d5 = d3 * d3 + d4 * d4;
                return d5 <= (double) (radius * radius);
            })
            .map(it -> (org.bukkit.entity.LivingEntity) it.bridge$getBukkitEntity())
            .toList();
        var event = CraftEventFactory.callAreaEffectCloudApplyEvent((AreaEffectCloud) (Object) this, affected);
        if (!event.isCancelled()) {
            return event.getAffectedEntities().stream().map(it -> ((CraftLivingEntity) it).getHandle()).collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }

    @Decorate(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$effectCause(LivingEntity instance, MobEffectInstance mobEffectInstance, Entity entity) throws Throwable {
        ((LivingEntityBridge) instance).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD);
        return (boolean) DecorationOps.callsite().invoke(instance, mobEffectInstance, entity);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;discard()V"))
    private void arclight$discard(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
