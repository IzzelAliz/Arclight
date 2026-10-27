package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.world.entity.vehicle.MinecartTNTBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartTNT.class)
public abstract class MinecartTNTMixin extends AbstractMinecartMixin implements MinecartTNTBridge {

    @Shadow public int fuse;

    public boolean isIncendiary;

    @Override
    public boolean bridge$isIncendiary() {
        return this.isIncendiary;
    }

    @Override
    public void bridge$setIncendiary(boolean value) {
        this.isIncendiary = value;
    }

    @Decorate(method = "explode(Lnet/minecraft/world/damagesource/DamageSource;D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void arclight$explode(ServerLevel level, Entity entity, DamageSource source, ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction) throws Throwable {
        var event = new ExplosionPrimeEvent(this.getBukkitEntity(), radius, this.isIncendiary);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.fuse = -1;
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(level, entity, source, calculator, x, y, z, event.getRadius(), event.getFire(), interaction);
    }

    @Inject(method = "explode(Lnet/minecraft/world/damagesource/DamageSource;D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartTNT;discard()V"))
    private void arclight$explodeCause(DamageSource damageSource, double d, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
    }
}
