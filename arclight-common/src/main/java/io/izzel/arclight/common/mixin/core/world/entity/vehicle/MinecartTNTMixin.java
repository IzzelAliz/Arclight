package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.mixin.Eject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartTNT.class)
public abstract class MinecartTNTMixin extends AbstractMinecartMixin {

    @Shadow private int fuse;

    @Eject(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion arclight$explode(Level level, Entity entity, double x, double y, double z, float radius, Explosion.BlockInteraction interaction, CallbackInfo ci) {
        var event = new ExplosionPrimeEvent(this.getBukkitEntity(), radius, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.fuse = -1;
            ci.cancel();
            return null;
        }
        return level.explode((MinecartTNT) (Object) this, x, y, z, event.getRadius(), event.getFire(), interaction);
    }
}
