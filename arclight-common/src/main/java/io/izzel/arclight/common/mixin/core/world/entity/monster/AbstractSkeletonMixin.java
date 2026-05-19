package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends PathfinderMobMixin {

    @Decorate(method = "performRangedAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile arclight$shootBow(Projectile arrowEntity, ServerLevel serverLevel, ItemStack projectile, double x, double y, double z, float velocity, float inaccuracy, LivingEntity target, float distanceFactor) throws Throwable {
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent((AbstractSkeleton) (Object) this, this.getMainHandItem(), null, arrowEntity, InteractionHand.MAIN_HAND, 0.8F, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            return (Projectile) DecorationOps.cancel().invoke();
        }
        if (event.getProjectile() != arrowEntity.bridge$getBukkitEntity()) {
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
            return (Projectile) DecorationOps.cancel().invoke();
        }
        return (Projectile) DecorationOps.callsite().invoke(arrowEntity, serverLevel, projectile, x, y, z, velocity, inaccuracy);
    }
}
