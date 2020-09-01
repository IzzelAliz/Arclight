package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.AbstractSkeletonEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractSkeletonEntity.class)
public abstract class AbstractSkeletonEntityMixin extends CreatureEntityMixin {

    @Inject(method = "attackEntityWithRangedAttack", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/AbstractSkeletonEntity;playSound(Lnet/minecraft/util/SoundEvent;FF)V"))
    private void arclight$shootBow(LivingEntity target, float distanceFactor, CallbackInfo ci, ItemStack itemStack, AbstractArrowEntity arrowEntity) {
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent((AbstractSkeletonEntity) (Object) this, this.getHeldItemMainhand(), null, arrowEntity, Hand.MAIN_HAND, 0.8F, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
            return;
        }
        if (event.getProjectile() != ((EntityBridge) arrowEntity).bridge$getBukkitEntity()) {
            this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
            ci.cancel();
        }
    }
}
