package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Shulker.class)
public abstract class ShulkerMixin extends PathfinderMobMixin {

    // @formatter:off
    @Shadow @Nullable protected abstract Direction findAttachableSurface(BlockPos p_149811_);
    @Shadow protected abstract void setAttachFace(Direction p_149789_);
    @Shadow @Final protected static EntityDataAccessor<Byte> DATA_PEEK_ID;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected boolean teleportSomewhere() {
        if (!this.isNoAi() && this.isAlive()) {
            BlockPos blockpos = this.blockPosition();

            for (int i = 0; i < 5; ++i) {
                BlockPos blockpos1 = blockpos.offset(Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8));
                if (blockpos1.getY() > this.level.getMinBuildHeight() && this.level.isEmptyBlock(blockpos1) && this.level.getWorldBorder().isWithinBounds(blockpos1) && this.level.noCollision((Shulker) (Object) this, (new AABB(blockpos1)).deflate(1.0E-6D))) {
                    Direction direction = this.findAttachableSurface(blockpos1);
                    if (direction != null) {
                        EntityTeleportEvent teleport = new EntityTeleportEvent(this.getBukkitEntity(), this.getBukkitEntity().getLocation(), new Location(((WorldBridge) this.level).bridge$getWorld(), blockpos1.getX(), blockpos1.getY(), blockpos1.getZ()));
                        Bukkit.getPluginManager().callEvent(teleport);
                        if (!teleport.isCancelled()) {
                            Location to = teleport.getTo();
                            blockpos1 = new BlockPos(to.getX(), to.getY(), to.getZ());
                        } else {
                            return false;
                        }
                        this.unRide();
                        this.setAttachFace(direction);
                        this.playSound(SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
                        this.setPos((double) blockpos1.getX() + 0.5D, blockpos1.getY(), (double) blockpos1.getZ() + 0.5D);
                        this.entityData.set(DATA_PEEK_ID, (byte) 0);
                        this.setTarget(null);
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Inject(method = "hitByShulkerBullet", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$breedCause(CallbackInfo ci) {
        ((WorldBridge) this.level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
    }
}
