package io.izzel.arclight.common.mixin.core.world.entity.ai.navigation;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Set;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    @Shadow @Final protected Mob mob;

    @Unique protected Entity arclight$pathfindTarget;

    @Decorate(method = "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;"), require = 1, allow = 1)
    private Path arclight$entityPathTarget(PathNavigation navigation, Set<BlockPos> targets, int radiusOffset, boolean above, int reachRange,
                                         @Local(ordinal = 0) Entity target) throws Throwable {
        Entity previousTarget = this.arclight$pathfindTarget;
        this.arclight$pathfindTarget = target;
        try {
            return (Path) DecorationOps.callsite().invoke(navigation, targets, radiusOffset, above, reachRange);
        } finally {
            this.arclight$pathfindTarget = previousTarget;
        }
    }

    @Decorate(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
        at = @At("HEAD"), inject = true, require = 1, allow = 1)
    private void arclight$capturePathTarget(@Local(allocate = "arclightPathfindTarget") Entity target) throws Throwable {
        // Consume attribution before callbacks; an ordinary nested block request has no entity target.
        target = this.arclight$pathfindTarget;
        this.arclight$pathfindTarget = null;
        DecorationOps.blackhole().invoke(target);
    }

    @Decorate(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/Profiler;get()Lnet/minecraft/util/profiling/ProfilerFiller;"),
        inject = true, require = 1, allow = 1)
    private void arclight$pathfindEvent(@Local(ordinal = 0) Set<BlockPos> targets,
                                      @Local(allocate = "arclightPathfindTarget") Entity target) throws Throwable {
        // The native empty/below-world/canUpdatePath/cached-path returns stay before this producer.
        Set<BlockPos> originalTargets = targets;
        boolean copiedSet = false;
        for (BlockPos possibleTarget : originalTargets) {
            if (!this.mob.level().getWorldBorder().isWithinBounds(possibleTarget)
                || !new EntityPathfindEvent(((EntityBridge) this.mob).bridge$getBukkitEntity(),
                    CraftLocation.toBukkit(possibleTarget, this.mob.level()),
                    target == null ? null : ((EntityBridge) target).bridge$getBukkitEntity()).callEvent()) {
                if (!copiedSet) {
                    copiedSet = true;
                    targets = new HashSet<>(targets);
                }
                targets.remove(possibleTarget);
                if (targets.isEmpty()) {
                    DecorationOps.cancel().invoke((Path) null);
                    return;
                }
            }
        }
        // Preserve the actual native/mod path finder, but supply only accepted candidates.
        DecorationOps.blackhole().invoke(targets);
    }
}
