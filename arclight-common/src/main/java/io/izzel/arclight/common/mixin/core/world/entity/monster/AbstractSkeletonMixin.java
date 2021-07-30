package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends PathfinderMobMixin {

    @Inject(method = "performRangedAttack", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void arclight$shootBow(LivingEntity target, float distanceFactor, CallbackInfo ci, ItemStack itemStack, AbstractArrow arrowEntity) {
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent((AbstractSkeleton) (Object) this, this.getMainHandItem(), null, arrowEntity, InteractionHand.MAIN_HAND, 0.8F, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
            return;
        }
        if (event.getProjectile() != ((EntityBridge) arrowEntity).bridge$getBukkitEntity()) {
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            ci.cancel();
        }
    }
}
