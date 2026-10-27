package io.izzel.arclight.common.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.EntityEffectTickEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceTickEventMixin {

    @Shadow @Final private Holder<MobEffect> effect;
    @Shadow private int amplifier;

    @Decorate(method = "tickServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/Runnable;)Z",
        require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"))
    private boolean arclight$effectTickEvent(MobEffect mobEffect, int tickCount, int amplification,
                                            @Local(ordinal = 0) LivingEntity target) throws Throwable {
        // A veto skips only this application, not duration/hidden-effect ticking.
        // Dispatch before the caller reads applyEffectTick's receiver/arguments.
        return (boolean) DecorationOps.callsite().invoke(mobEffect, tickCount, amplification)
            && new EntityEffectTickEvent(
                ((LivingEntityBridge) target).bridge$getBukkitEntity(),
                CraftPotionEffectType.minecraftHolderToBukkit(this.effect), this.amplifier
            ).callEvent();
    }
}
