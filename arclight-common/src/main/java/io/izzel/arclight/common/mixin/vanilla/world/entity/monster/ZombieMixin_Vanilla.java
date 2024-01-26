package io.izzel.arclight.common.mixin.vanilla.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Zombie.class)
public class ZombieMixin_Vanilla {

    @Inject(method = "hurt", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Zombie;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$spawnWithReason(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir, ServerLevel world, LivingEntity livingEntity, int i, int j, int k, net.minecraft.world.entity.monster.Zombie zombieEntity) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.REINFORCEMENTS);
        if (livingEntity != null) {
            ((MobEntityBridge) zombieEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.REINFORCEMENT_TARGET, true);
        }
    }
}
