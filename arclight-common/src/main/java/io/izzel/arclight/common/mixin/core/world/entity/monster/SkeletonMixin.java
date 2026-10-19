package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Skeleton.class)
public abstract class SkeletonMixin extends PathfinderMobMixin {

    // Decorate the native call, not doFreezeConversion itself: Neo's canLivingConvert
    // veto runs before this site and must still be able to prevent the conversion.
    @SuppressWarnings("unchecked")
    @Decorate(method = "doFreezeConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/skeleton/Skeleton;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> T arclight$freezeConversion(Skeleton skeleton, EntityType<T> entityType,
                                                          ConversionParams params, ConversionParams.AfterConversion<T> afterConversion) throws Throwable {
        WorldBridge world = (WorldBridge) skeleton.level();
        CreatureSpawnEvent.SpawnReason previousReason = world.bridge$getAddEntityReason();
        var previousTransform = this.bridge$getTransformReason();
        this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.FROZEN);
        world.bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.FROZEN);
        try {
            T converted = (T) DecorationOps.callsite().invoke(skeleton, entityType, params, afterConversion);
            if (converted == null) {
                ((org.bukkit.entity.Skeleton) this.bridge$getBukkitEntity()).setConversionTime(-1);
            }
            return converted;
        } finally {
            // Mob's RETURN cleanup does not execute on listener exceptions. Never leak
            // FROZEN into a later conversion/add, even if event dispatch throws.
            this.bridge$pushTransformReason(previousTransform);
            world.bridge$pushAddEntityReason(previousReason);
        }
    }
}
