package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.EggEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Egg;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EggEntity.class)
public abstract class EggEntityMixin extends ThrowableEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onImpact(final RayTraceResult movingobjectposition) {
        if (movingobjectposition.getType() == RayTraceResult.Type.ENTITY) {
            ((EntityRayTraceResult) movingobjectposition).getEntity().attackEntityFrom(DamageSource.causeThrownDamage((EggEntity) (Object) this, this.getThrower()), 0.0f);
        }
        if (!this.world.isRemote) {
            boolean hatching = this.rand.nextInt(8) == 0;
            byte b0 = 1;
            if (this.rand.nextInt(32) == 0) {
                b0 = 4;
            }
            if (!hatching) {
                b0 = 0;
            }
            org.bukkit.entity.EntityType hatchingType = org.bukkit.entity.EntityType.CHICKEN;
            final Entity shooter = this.getThrower();
            if (shooter instanceof ServerPlayerEntity) {
                final PlayerEggThrowEvent event = new PlayerEggThrowEvent(((ServerPlayerEntityBridge) shooter).bridge$getBukkitEntity(), (Egg) this.getBukkitEntity(), hatching, b0, hatchingType);
                Bukkit.getPluginManager().callEvent(event);
                b0 = event.getNumHatches();
                hatching = event.isHatching();
                hatchingType = event.getHatchingType();
            }
            if (hatching) {
                for (int i = 0; i < b0; ++i) {
                    Entity entity = ((CraftEntity) ((WorldBridge) this.world).bridge$getWorld().spawnEntity(new Location(((WorldBridge) this.world).bridge$getWorld(), this.posX, this.posY, this.posZ, this.rotationYaw, 0.0f), hatchingType)).getHandle();
                    if (((EntityBridge) entity).bridge$getBukkitEntity() instanceof Ageable) {
                        ((Ageable) ((EntityBridge) entity).bridge$getBukkitEntity()).setBaby();
                    }
                    ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EGG);
                    this.world.getWorld().addEntity(entity);
                }
            }
            this.world.setEntityState((EggEntity) (Object) this, (byte) 3);
            this.remove();
        }
    }
}
