package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMoveEventMixin {

    @Decorate(method = "aiStep()V", inject = true, require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0, shift = At.Shift.AFTER),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;pushEntities()V")))
    private void arclight$entityMoveEvent() {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof Player
            || EntityMoveEvent.getHandlerList().getRegisteredListeners().length == 0) {
            return;
        }
        if (entity.xo == entity.getX() && entity.yo == entity.getY() && entity.zo == entity.getZ()
            && entity.yRotO == entity.getYRot() && entity.xRotO == entity.getXRot()) {
            return;
        }

        // Match Paper's post-push boundary, before the water-sensitive damage tail.
        World world = ((WorldBridge) level).bridge$getWorld();
        Location from = new Location(world, entity.xo, entity.yo, entity.zo, entity.yRotO, entity.xRotO);
        Location to = new Location(world, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        EntityMoveEvent event = new EntityMoveEvent(((LivingEntityBridge) entity).bridge$getBukkitEntity(), from, to.clone());
        if (!event.callEvent()) {
            // Paper retains this original reference: in-place getFrom() edits apply,
            // but replacing event.from with setFrom() does not replace this snapshot.
            entity.absSnapTo(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        } else if (!to.equals(event.getTo())) {
            Location destination = event.getTo();
            entity.absSnapTo(destination.getX(), destination.getY(), destination.getZ(), destination.getYaw(), destination.getPitch());
        }
    }
}
