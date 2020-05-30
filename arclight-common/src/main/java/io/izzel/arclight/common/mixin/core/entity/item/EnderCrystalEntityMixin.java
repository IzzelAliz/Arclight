package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderCrystalEntity.class)
public abstract class EnderCrystalEntityMixin extends EntityMixin {

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private void arclight$blockIgnite(CallbackInfo ci) {
        if (CraftEventFactory.callBlockIgniteEvent(this.world, this.getPosition(), (EnderCrystalEntity) (Object) this).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EnderCrystalEntity;remove()V"))
    private void arclight$entityDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((EnderCrystalEntity) (Object)this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;"))
    private Explosion arclight$blockPrime(World world, Entity entityIn, double xIn, double yIn, double zIn, float explosionRadius, Explosion.Mode modeIn) {
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), explosionRadius, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.removed = false;
            return null;
        } else {
            return world.createExplosion(entityIn, xIn, yIn, zIn, event.getRadius(), event.getFire(), modeIn);
        }
    }
}
