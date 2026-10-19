package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MaceItem.class)
public abstract class MaceItemMixin {

    // Fixed Spigot dispatches ENTITY_ATTACK at this Mace-only nearby-target push.
    // Cancellation skips only push; the surrounding ServerPlayer packet tail remains.
    @WrapOperation(
        method = "lambda$knockback$0(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"),
        require = 1,
        expect = 1,
        allow = 1
    )
    private static void arclight$entityKnockbackEvent(LivingEntity nearby, double x, double y, double z,
                                                       Operation<Void> original,
                                                       @Local(argsOnly = true, index = 1) Entity attacker,
                                                       @Local(index = 4) double strength) {
        Vec3 pushIncrement = new Vec3(x, y, z);
        Vec3 rawVelocity = attacker.getDeltaMovement().add(pushIncrement);
        CraftLivingEntity bukkitNearby = (CraftLivingEntity) ((LivingEntityBridge) nearby).bridge$getBukkitEntity();
        EntityKnockbackEvent event = CraftEventFactory.callEntityKnockbackEvent(
            bukkitNearby, attacker, EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK,
            strength, rawVelocity, x, y, z
        );
        if (!event.isCancelled()) {
            var finalKnockback = event.getFinalKnockback();
            original.call(nearby, finalKnockback.getX(), finalKnockback.getY(), finalKnockback.getZ());
        }
    }
}
