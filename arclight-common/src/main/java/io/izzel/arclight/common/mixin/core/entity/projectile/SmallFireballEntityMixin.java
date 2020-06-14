package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin extends DamagingProjectileEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("RETURN"))
    private void arclight$init(World worldIn, LivingEntity shooter, double accelX, double accelY, double accelZ, CallbackInfo ci) {
        if (this.shootingEntity != null && this.shootingEntity instanceof MobEntity) {
            this.isIncendiary = this.world.getGameRules().getBoolean(GameRules.MOB_GRIEFING);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onImpact(RayTraceResult result) {
        if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
            super.onImpact(result);
        }
        if (!this.world.isRemote) {
            if (result.getType() == RayTraceResult.Type.ENTITY) {
                Entity entity = ((EntityRayTraceResult) result).getEntity();
                if (!entity.isImmuneToFire()) {
                    int i = entity.getFireTimer();
                    if (isIncendiary) {
                        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), 5);
                        Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled()) {
                            ((EntityBridge) entity).bridge$setOnFire(event.getDuration(), false);
                        }
                    }
                    boolean flag = entity.attackEntityFrom(DamageSource.causeFireballDamage((SmallFireballEntity) (Object) this, this.shootingEntity), 5.0F);
                    if (flag) {
                        this.applyEnchantments(this.shootingEntity, entity);
                    } else {
                        entity.setFireTimer(i);
                    }
                }
            } else if (isIncendiary && this.shootingEntity == null || !(this.shootingEntity instanceof MobEntity) || ForgeEventFactory.getMobGriefingEvent(this.world, this.shootingEntity)) {
                BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) result;
                BlockPos blockpos = blockraytraceresult.getPos().offset(blockraytraceresult.getFace());
                if (this.world.isAirBlock(blockpos) && !CraftEventFactory.callBlockIgniteEvent(this.world, blockpos, (SmallFireballEntity) (Object) this).isCancelled()) {
                    this.world.setBlockState(blockpos, Blocks.FIRE.getDefaultState());
                }
            }

            this.remove();
        }

    }
}
