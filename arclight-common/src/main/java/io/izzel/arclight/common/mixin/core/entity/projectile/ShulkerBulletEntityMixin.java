package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ShulkerBulletEntity.class)
public abstract class ShulkerBulletEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow private Entity target;
    @Shadow @Nullable private Direction direction;
    @Shadow protected abstract void selectNextMoveDirection(@Nullable Direction.Axis p_184569_1_);
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Direction$Axis;)V", at = @At("RETURN"))
    private void arclight$init(World worldIn, LivingEntity ownerIn, Entity targetIn, Direction.Axis p_i46772_4_, CallbackInfo ci) {
        this.projectileSource = ((LivingEntityBridge) ownerIn).bridge$getBukkitEntity();
    }

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$reason(EntityRayTraceResult result, CallbackInfo ci) {
        ((LivingEntityBridge) result.getEntity()).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At("HEAD"))
    private void arclight$damageBullet(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ShulkerBulletEntity) (Object) this, source, amount, false)) {
            cir.setReturnValue(false);
        }
    }

    public Entity getTarget() {
        return this.target;
    }

    public void setTarget(final Entity e) {
        this.target = e;
        this.direction = Direction.UP;
        this.selectNextMoveDirection(Direction.Axis.X);
    }
}
