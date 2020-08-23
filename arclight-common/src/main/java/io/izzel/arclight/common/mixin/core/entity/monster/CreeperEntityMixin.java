package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.monster.CreeperEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.Explosion;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends CreatureEntityMixin implements CreeperEntityBridge {

    // @formatter:off
    @Shadow @Final private static DataParameter<Boolean> POWERED;
    @Shadow public int explosionRadius;
    @Shadow protected abstract void spawnLingeringCloud();
    @Shadow private int timeSinceIgnited;
    // @formatter:on

    @Inject(method = "func_241841_a", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/monster/CreeperEntity;dataManager:Lnet/minecraft/network/datasync/EntityDataManager;"))
    private void arclight$lightningBolt(ServerWorld world, LightningBoltEntity lightningBolt, CallbackInfo ci) {
        if (CraftEventFactory.callCreeperPowerEvent((CreeperEntity) (Object) this, lightningBolt, CreeperPowerEvent.PowerCause.LIGHTNING).isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void explode() {
        if (!this.world.isRemote) {
            Explosion.Mode explosion_effect = ForgeEventFactory.getMobGriefingEvent(this.world, (CreeperEntity) (Object) this) ? Explosion.Mode.DESTROY : Explosion.Mode.NONE;
            final float f = this.dataManager.get(POWERED) ? 2.0f : 1.0f;
            final ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), this.explosionRadius * f, false);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.dead = true;
                this.world.createExplosion((CreeperEntity) (Object) this, this.getPosX(), this.getPosY(), this.getPosZ(), event.getRadius(), event.getFire(), explosion_effect);
                this.remove();
                this.spawnLingeringCloud();
            } else {
                this.timeSinceIgnited = 0;
            }
        }
    }

    @Inject(method = "spawnLingeringCloud", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$creeperCloud(CallbackInfo ci, Collection<EffectInstance> collection, AreaEffectCloudEntity areaeffectcloudentity) {
        areaeffectcloudentity.setOwner((CreeperEntity) (Object) this);
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EXPLOSION);
    }

    public void setPowered(boolean power) {
        this.dataManager.set(POWERED, power);
    }

    @Override
    public void bridge$setPowered(boolean power) {
        setPowered(power);
    }
}
