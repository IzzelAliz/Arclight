package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.monster.CreeperEntityBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;

@Mixin(Creeper.class)
public abstract class CreeperMixin extends PathfinderMobMixin implements CreeperEntityBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_POWERED;
    @Shadow public int explosionRadius;
    @Shadow protected abstract void spawnLingeringCloud();
    @Shadow private int swell;
    @Shadow public abstract boolean isPowered();
    // @formatter:on

    @Unique private Player entityIgniter; // CraftBukkit

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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public final void explodeCreeper() {
        if (!this.level().isClientSide) {
            final float f = this.isPowered() ? 2.0f : 1.0f;
            final ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), this.explosionRadius * f, false);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.dead = true;
                this.level().explode((Creeper) (Object) this,
                    ((DamageSourceBridge) Explosion.getDefaultDamageSource(level(), (Creeper) (Object) this)).bridge$customCausingEntity(entityIgniter), null,
                    this.getX(), this.getY(), this.getZ(), event.getRadius(), event.getFire(), Level.ExplosionInteraction.MOB);
                this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
                this.discard();
                this.spawnLingeringCloud();
            } else {
                this.swell = 0;
            }
        }
    }

    @Inject(method = "spawnLingeringCloud", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$creeperCloud(CallbackInfo ci, Collection<MobEffectInstance> collection, AreaEffectCloud areaeffectcloudentity) {
        areaeffectcloudentity.setOwner((Creeper) (Object) this);
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EXPLOSION);
    }

    public void setPowered(boolean power) {
        this.entityData.set(DATA_IS_POWERED, power);
    }

    @Override
    public void bridge$setPowered(boolean power) {
        setPowered(power);
    }
}
