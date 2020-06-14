package io.izzel.arclight.common.mixin.v1_15.entity.monster;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.entity.monster.ZombieEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ZombiePigmanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.entity.PigZombieAngerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombiePigmanEntity.class)
public abstract class ZombiePigmanEntityMixin_1_15 extends ZombieEntityMixin {

    // @formatter:off
    @Shadow protected abstract boolean func_226547_i_(LivingEntity p_70835_1_);
    @Shadow protected abstract int func_223336_ef();
    // @formatter:on

    public boolean attackEntityFrom(DamageSource damagesource, float f) {
        if (this.isInvulnerableTo(damagesource)) {
            return false;
        }
        Entity entity = damagesource.getTrueSource();
        boolean result = super.attackEntityFrom(damagesource, f);
        if (result && entity instanceof PlayerEntity && !((PlayerEntity) entity).isCreative() && this.canEntityBeSeen(entity)) {
            this.func_226547_i_((LivingEntity) entity);
        }
        return result;
    }

    @Inject(method = "func_226547_i_", cancellable = true, at = @At("HEAD"))
    private void arclight$anger(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        PigZombieAngerEvent event = new PigZombieAngerEvent((PigZombie) this.getBukkitEntity(), (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), this.func_223336_ef());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        } else {
            arclight$capture = event.getNewAnger();
        }
    }

    private transient int arclight$capture;

    @Redirect(method = "func_226547_i_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombiePigmanEntity;func_223336_ef()I"))
    private int arclight$useAnger(ZombiePigmanEntity pigmanEntity) {
        return arclight$capture;
    }
}
