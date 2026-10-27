package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.EntityAttemptSpinAttackEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpinAttackEventMixin {

    @Shadow protected int autoSpinAttackTicks;

    @Decorate(method = "checkAutoSpinAttack(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/AABB;)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$initSpinSkips(@Local(allocate = "arclightSpinSkipped") int skipped) throws Throwable {
        skipped = 0;
        DecorationOps.blackhole().invoke(skipped);
    }

    @Decorate(method = "checkAutoSpinAttack(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/AABB;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), require = 1, allow = 1)
    private boolean arclight$countSpinCandidates(List<Entity> entities,
                                                 @Local(allocate = "arclightSpinCandidates") int candidates) throws Throwable {
        candidates = entities.size();
        DecorationOps.blackhole().invoke(candidates);
        return (boolean) DecorationOps.callsite().invoke(entities);
    }

    // A veto is a continue, not a return: another candidate can still be attacked.
    // Keep the native virtual attack, item/damage callbacks and bounce tail intact.
    @Decorate(method = "checkAutoSpinAttack(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/AABB;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), require = 1, allow = 1)
    private Object arclight$attemptSpinAttack(Iterator<Entity> iterator,
                                             @Local(allocate = "arclightSpinSkipped") int skipped) throws Throwable {
        Object next = DecorationOps.callsite().invoke(iterator);
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            Entity entity = (Entity) next;
            boolean hidden = self instanceof ServerPlayer player
                && !((org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity())
                    .canSee(((EntityBridge) entity).bridge$getBukkitEntity());
            if (hidden || entity instanceof LivingEntity living
                && !new EntityAttemptSpinAttackEvent(((LivingEntityBridge) self).bridge$getBukkitEntity(),
                    ((LivingEntityBridge) living).bridge$getBukkitEntity()).callEvent()) {
                ++skipped;
                DecorationOps.blackhole().invoke(skipped);
                throw DecorationOps.jumpToLoopStart();
            }
        }
        return next;
    }

    // The native empty-list collision branch is retained. Paper additionally treats
    // a list containing only hidden/vetoed candidates as empty, before flag cleanup.
    @Decorate(method = "checkAutoSpinAttack(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/AABB;)V",
        inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"),
        require = 1, allow = 1)
    private void arclight$finishSkippedSpin(@Local(allocate = "arclightSpinCandidates") int candidates,
                                           @Local(allocate = "arclightSpinSkipped") int skipped) {
        if (skipped > 0 && skipped == candidates && ((LivingEntity) (Object) this).horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }
    }
}
