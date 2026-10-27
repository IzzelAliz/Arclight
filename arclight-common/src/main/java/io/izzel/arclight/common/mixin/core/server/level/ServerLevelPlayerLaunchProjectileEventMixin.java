package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelPlayerLaunchProjectileEventMixin {

    @Decorate(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", inject = true, at = @At("HEAD"))
    private void arclight$playerLaunchBeforeAdmission(Entity entity) throws Throwable {
        if (entity instanceof ProjectileSpawnBridge projectile && !projectile.bridge$callLaunchEvent()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
    }
}
