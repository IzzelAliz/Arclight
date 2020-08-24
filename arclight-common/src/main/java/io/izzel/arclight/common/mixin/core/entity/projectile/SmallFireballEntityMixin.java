package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin extends AbstractFireballEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("RETURN"))
    private void arclight$init(World worldIn, LivingEntity shooter, double accelX, double accelY, double accelZ, CallbackInfo ci) {
        if (this.func_234616_v_() != null && this.func_234616_v_() instanceof MobEntity) {
            this.isIncendiary = this.world.getGameRules().getBoolean(GameRules.MOB_GRIEFING);
        }
    }

    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    private void arclight$entityCombust(Entity entity, int seconds) {
        if (this.isIncendiary) {
            EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), seconds);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                ((EntityBridge) entity).bridge$setOnFire(event.getDuration(), false);
            }
        }
    }

    @Inject(method = "func_230299_a_", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private void arclight$burnBlock(BlockRayTraceResult result, CallbackInfo ci, Entity entity, BlockPos pos) {
        if (!this.isIncendiary || CraftEventFactory.callBlockIgniteEvent(this.world, pos, (SmallFireballEntity) (Object) this).isCancelled()) {
            ci.cancel();
        }
    }
}
