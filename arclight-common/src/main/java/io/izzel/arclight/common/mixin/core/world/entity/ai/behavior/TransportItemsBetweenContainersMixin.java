package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {

    @Decorate(method = "isTargetValidToPick(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/util/Set;Ljava/util/Set;Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers$TransportItemTarget;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers;isContainerLocked(Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers$TransportItemTarget;)Z"), require = 1, allow = 1)
    private boolean arclight$validateTransportTarget(TransportItemsBetweenContainers behavior, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob body, Level level) throws Throwable {
        boolean locked = (boolean) DecorationOps.callsite().invoke(behavior, target);
        // Reuse the native rejection branch without stopping the remaining container search.
        return locked || !CraftEventFactory.callTransporterValidateTarget(body, level, target.pos());
    }
}
