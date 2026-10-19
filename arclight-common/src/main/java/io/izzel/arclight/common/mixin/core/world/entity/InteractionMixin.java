package io.izzel.arclight.common.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.advancements.criterion.PlayerHurtEntityTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Interaction.class)
public class InteractionMixin {
    @Inject(method = "skipAttackInteraction", cancellable = true, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/Interaction;attack:Lnet/minecraft/world/entity/Interaction$PlayerAction;"))
    private void arclight$onDamage(Entity entity, CallbackInfoReturnable<Boolean> cir,
            @Share("arclight$source") LocalRef<DamageSource> source,
            @Share("arclight$damage") LocalFloatRef damage) {
        DamageSource attackSource = entity.damageSources().playerAttack((Player) entity);
        var event = CraftEventFactory.callNonLivingEntityDamageEvent((Entity) (Object) this, attackSource, 1.0F, false);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
        } else {
            source.set(attackSource);
            damage.set((float) event.getFinalDamage());
        }
    }

    @WrapOperation(method = "skipAttackInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/criterion/PlayerHurtEntityTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FFZ)V"))
    private void arclight$criterionDamage(PlayerHurtEntityTrigger trigger, ServerPlayer player, Entity target,
            DamageSource nativeSource, float dealt, float taken, boolean blocked, Operation<Void> original,
            @Share("arclight$source") LocalRef<DamageSource> source,
            @Share("arclight$damage") LocalFloatRef damage) {
        original.call(trigger, player, target, source.get(), damage.get(), taken, blocked);
    }
}
