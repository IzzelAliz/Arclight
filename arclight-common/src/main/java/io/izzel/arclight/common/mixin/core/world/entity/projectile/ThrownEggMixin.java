package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.HitResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Egg;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ThrownEgg.class)
public abstract class ThrownEggMixin extends ThrowableProjectileMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onHit(final HitResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            boolean hatching = this.random.nextInt(8) == 0;
            byte b0 = 1;
            if (this.random.nextInt(32) == 0) {
                b0 = 4;
            }
            if (!hatching) {
                b0 = 0;
            }
            org.bukkit.entity.EntityType hatchingType = org.bukkit.entity.EntityType.CHICKEN;
            Entity shooter = this.getOwner();
            if (shooter instanceof ServerPlayer) {
                PlayerEggThrowEvent event = new PlayerEggThrowEvent(((ServerPlayerEntityBridge) shooter).bridge$getBukkitEntity(), (Egg) this.getBukkitEntity(), hatching, b0, hatchingType);
                Bukkit.getPluginManager().callEvent(event);
                b0 = event.getNumHatches();
                hatching = event.isHatching();
                hatchingType = event.getHatchingType();
            }
            if (hatching) {
                for (int i = 0; i < b0; ++i) {
                    Entity entity = ((WorldBridge) this.level).bridge$getWorld().createEntity(new Location(((WorldBridge) this.level).bridge$getWorld(), this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f), hatchingType.getEntityClass());
                    if (((EntityBridge) entity).bridge$getBukkitEntity() instanceof Ageable) {
                        ((Ageable) ((EntityBridge) entity).bridge$getBukkitEntity()).setBaby();
                    }
                    ((WorldBridge) this.level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EGG);
                    this.level.addFreshEntity(entity);
                }
            }
            this.level.broadcastEntityEvent((ThrownEgg) (Object) this, (byte) 3);
            this.discard();
        }
    }
}
