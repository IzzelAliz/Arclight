package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.ShulkerDuplicateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Shulker.class)
public abstract class ShulkerMixin extends PathfinderMobMixin {

    @Decorate(method = "teleportSomewhere", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Shulker;unRide()V"))
    private void arclight$teleportEvent(Shulker instance, @Local(ordinal = -1) BlockPos pos) throws Throwable {
        EntityTeleportEvent teleport = new EntityTeleportEvent(this.getBukkitEntity(), this.getBukkitEntity().getLocation(), new Location(this.level().bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(teleport);
        if (!teleport.isCancelled()) {
            Location to = teleport.getTo();
            pos = BlockPos.containing(to.getX(), to.getY(), to.getZ());
        } else {
            DecorationOps.cancel().invoke(false);
            return;
        }
        DecorationOps.blackhole().invoke(pos);
        DecorationOps.callsite().invoke(instance);
    }

    @Decorate(method = "hitByShulkerBullet()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), require = 1, allow = 1)
    private boolean arclight$duplicateEvent(Level level, Entity child) throws Throwable {
        ShulkerDuplicateEvent event = new ShulkerDuplicateEvent(
            (org.bukkit.entity.Shulker) ((EntityBridge) child).bridge$getBukkitEntity(),
            (org.bukkit.entity.Shulker) this.getBukkitEntity());
        if (!event.callEvent()) {
            // The parent has already teleported; Paper only vetoes the child's admission.
            return (boolean) DecorationOps.cancel().invoke();
        }
        ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
        return (boolean) DecorationOps.callsite().invoke(level, child);
    }
}
