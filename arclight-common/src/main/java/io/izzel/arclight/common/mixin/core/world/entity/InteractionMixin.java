package io.izzel.arclight.common.mixin.core.world.entity;

import net.minecraft.advancements.critereon.PlayerHurtEntityTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Interaction.class)
public class InteractionMixin {

    private double arclight$finalDamage;

    @Inject(method = "skipAttackInteraction", cancellable = true, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/Interaction;attack:Lnet/minecraft/world/entity/Interaction$PlayerAction;"))
    private void arclight$onDamage(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        DamageSource source = entity.damageSources().playerAttack((Player) entity);
        var event = CraftEventFactory.callNonLivingEntityDamageEvent((Entity) (Object) this, source, 1.0F, false);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
        } else {
            arclight$finalDamage = event.getFinalDamage();
        }
    }

    @Redirect(method = "skipAttackInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/PlayerHurtEntityTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FFZ)V"))
    private void arclight$setDamage(PlayerHurtEntityTrigger instance, ServerPlayer p_60113_, Entity p_60114_, DamageSource p_60115_, float p_60116_, float p_60117_, boolean p_60118_) {
        instance.trigger(p_60113_, p_60114_, p_60115_, (float) arclight$finalDamage, p_60117_, p_60118_);
    }
}
