package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.passive.TameableEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FollowOwnerGoal.class)
public class FollowOwnerGoalMixin {

    // @formatter:off
    @Shadow @Final private TameableEntity tameable;
    // @formatter:on

    private transient boolean arclight$cancelled;

    @Redirect(method = "tryToTeleportToLocation", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/TameableEntity;setLocationAndAngles(DDDFF)V"))
    public void arclight$teleport(TameableEntity tameableEntity, double x, double y, double z, float yaw, float pitch) {
        CraftEntity craftEntity = ((EntityBridge) this.tameable).bridge$getBukkitEntity();
        Location location = new Location(craftEntity.getWorld(), x, y, z, yaw, pitch);
        EntityTeleportEvent event = new EntityTeleportEvent(craftEntity, craftEntity.getLocation(), location);
        Bukkit.getPluginManager().callEvent(event);
        if (!(arclight$cancelled = event.isCancelled())) {
            tameableEntity.setLocationAndAngles(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch());
        }
    }

    @Inject(method = "tryToTeleportToLocation", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/pathfinding/PathNavigator;clearPath()V"))
    public void arclight$returnIfFail(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (arclight$cancelled) {
            cir.setReturnValue(false);
        }
        arclight$cancelled = false;
    }
}
