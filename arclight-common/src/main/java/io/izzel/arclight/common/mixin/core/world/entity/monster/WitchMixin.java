package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.mixin.core.world.entity.raider.RaiderMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(Witch.class)
public abstract class WitchMixin extends RaiderMixin {

    @Decorate(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionContents;forEachEffect(Ljava/util/function/Consumer;)V"))
    private void arclight$reason(PotionContents instance, Consumer<MobEffectInstance> consumer) throws Throwable {
        Consumer<MobEffectInstance> wrapped = effect -> {
            bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
            consumer.accept(effect);
        };
        DecorationOps.callsite().invoke(instance, wrapped);
    }
}
