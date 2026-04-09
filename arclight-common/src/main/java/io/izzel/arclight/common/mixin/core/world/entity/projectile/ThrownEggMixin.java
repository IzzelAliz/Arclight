package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.bukkit.EntityTypeBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.Blackhole;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.HitResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Egg;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThrownEgg.class)
public abstract class ThrownEggMixin extends ThrowableProjectileMixin {

    @Shadow @Final private static EntityDimensions ZERO_SIZED_DIMENSIONS;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onHit(final HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            boolean hatching = this.random.nextInt(8) == 0;
            int hatches = 1;
            if (this.random.nextInt(32) == 0) {
                hatches = 4;
            }
            if (!hatching) {
                hatches = 0;
            }
            org.bukkit.entity.EntityType hatchingType = org.bukkit.entity.EntityType.CHICKEN;
            Entity shooter = this.getOwner();
            if (shooter instanceof ServerPlayer) {
                PlayerEggThrowEvent event = new PlayerEggThrowEvent(((ServerPlayerEntityBridge) shooter).bridge$getBukkitEntity(), (Egg) this.getBukkitEntity(), hatching, (byte) hatches, hatchingType);
                Bukkit.getPluginManager().callEvent(event);
                hatches = event.getNumHatches();
                hatching = event.isHatching();
                hatchingType = event.getHatchingType();
            }
            if (hatching && hatchingType == org.bukkit.entity.EntityType.CHICKEN) {
                for (int i = 0; i < hatches; ++i) {
                    // Preserve an explicit Chicken local and the vanilla-like hatch path for mod compatibility.
                    // - Meadow / Let's Do keeps relying on a Chicken local-store shape (#1149).
                    // - VanillaBackport captures locals around addFreshEntity(Chicken) on this path (#2112).
                    Chicken chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(this.level());
                    if (chicken != null) {
                        Blackhole.consume(chicken);
                        chicken.setAge(-24000);
                        chicken.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        if (!chicken.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
                            break;
                        }
                        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EGG);
                        this.level().addFreshEntity(chicken);
                    }
                }
            } else if (hatching) {
                for (int i = 0; i < hatches; ++i) {
                    // TrickOrTreatMod compat https://github.com/IzzelAliz/Arclight/issues/1178
                    // https://github.com/MehVahdJukaar/TrickOrTreatMod/blob/020bc478b8f8de6bfec2191a9e667f423f45d7db/common/src/main/java/net/mehvahdjukaar/hauntedharvest/mixins/ThrownEggEntityMixin.java
                    // Keep the non-chicken fallback on EntityType#create(Level) for existing compat expectations.
                    var entityType = ((EntityTypeBridge) (Object) hatchingType).bridge$getHandle();
                    var entity = entityType.create(this.level());
                    // Let's do: Meadow mixin compatibility https://github.com/IzzelAliz/Arclight/issues/1149
                    if (entity instanceof Chicken) {
                        Chicken chicken = (Chicken) entity;
                        Blackhole.consume(chicken);
                    }
                    if (entity != null) {
                        if (((EntityBridge) entity).bridge$getBukkitEntity() instanceof Ageable) {
                            ((Ageable) ((EntityBridge) entity).bridge$getBukkitEntity()).setBaby();
                        }
                        entity.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        if (!entity.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
                            break;
                        }
                        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EGG);
                        this.level().addFreshEntity(entity);
                    }
                }
            }
            this.level().broadcastEntityEvent((ThrownEgg) (Object) this, (byte) 3);
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
            this.discard();
        }
    }
}
