package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import io.izzel.arclight.common.bridge.core.world.entity.monster.CreeperBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;

@Mixin(Creeper.class)
public abstract class CreeperMixin extends PathfinderMobMixin implements CreeperBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_POWERED;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;
    @Shadow public int explosionRadius;
    @Shadow protected abstract void spawnLingeringCloud();
    @Shadow private int swell;
    @Shadow public abstract boolean isPowered();
    @Shadow public abstract boolean isIgnited();
    // @formatter:on

    public Entity entityIgniter; // CraftBukkit
    @Override
    public Entity bridge$getIgniter() {
        return this.entityIgniter;
    }

    @Override
    public void bridge$setIgniter(Entity igniter) {
        this.entityIgniter = igniter;
    }

    @Unique private float eventRadius = 0f;
    @Unique private boolean eventFire = false;

    @Inject(method = "ignite()V", at = @At("HEAD"), cancellable = true)
    private void arclight$ignite(CallbackInfo ci) {
        this.bridge$setIgnited(true);
        ci.cancel();
    }

    @Redirect(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V"))
    private void arclight$restoreIgnited(Creeper instance) {
        // Restore persisted state without firing CreeperIgniteEvent.
        instance.getEntityData().set(DATA_IS_IGNITED, true);
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V"))
    private void arclight$catchIgniter(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        this.entityIgniter = player;
    }

    @Inject(method = "thunderHit", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/Creeper;entityData:Lnet/minecraft/network/syncher/SynchedEntityData;"))
    private void arclight$lightningBolt(ServerLevel world, LightningBolt lightningBolt, CallbackInfo ci) {
        if (CraftEventFactory.callCreeperPowerEvent((Creeper) (Object) this, lightningBolt, CreeperPowerEvent.PowerCause.LIGHTNING).isCancelled()) {
            ci.cancel();
        }
    }

    /*
     * @author IzzelAliz
     * @reason
     */
    //Support Sophisticated Backpacks injecting into explode()
    //@Overwrite
    //public void explodeCreeper()

    @Decorate(method = "explodeCreeper()V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/monster/Creeper;dead:Z"))
    private void arclight$delaySetDead(Creeper instance, boolean value, @Local(ordinal = -1) float f) throws Throwable {
        final ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), this.explosionRadius * f, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.swell = 0;
            this.entityData.set(DATA_IS_IGNITED, false);
            DecorationOps.cancel().invoke();
            return;
        }
        this.eventRadius = event.getRadius();
        this.eventFire = event.getFire();
        DecorationOps.callsite().invoke(instance, value);
    }

    // Use Redirect to preserve injection point
    @Redirect(method = "explodeCreeper", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void arclight$redirectExplode(ServerLevel instance, Entity arg, double d, double e, double f, float g, Level.ExplosionInteraction arg2) {
        instance.explode((Creeper) (Object) this,
                ((DamageSourceBridge) instance.damageSources().explosion((Creeper) (Object) this, (Creeper) (Object) this)).bridge$customCausingEntityDamager(entityIgniter), null,
                this.getX(), this.getY(), this.getZ(), eventRadius, eventFire, Level.ExplosionInteraction.MOB);
    }

    @Inject(method = "spawnLingeringCloud", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$creeperCloud(CallbackInfo ci, Collection<MobEffectInstance> collection, AreaEffectCloud areaeffectcloudentity) {
        areaeffectcloudentity.setOwner((Creeper) (Object) this);
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EXPLOSION);
    }

    @Override
    public void bridge$setIgnited(boolean ignited) {
        if (this.isIgnited() != ignited) {
            CreeperIgniteEvent event = new CreeperIgniteEvent((org.bukkit.entity.Creeper) this.getBukkitEntity(), ignited);
            if (event.callEvent()) {
                this.entityData.set(DATA_IS_IGNITED, event.isIgnited());
            }
        }
    }

    public void setPowered(boolean power) {
        this.entityData.set(DATA_IS_POWERED, power);
    }

    @Override
    public void bridge$setPowered(boolean power) {
        setPowered(power);
    }

    public void setIgnited(boolean ignited) {
        this.bridge$setIgnited(ignited);
    }

}
