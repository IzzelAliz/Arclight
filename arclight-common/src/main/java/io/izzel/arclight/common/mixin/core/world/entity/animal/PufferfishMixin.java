package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.PufferFishStateChangeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.player.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pufferfish.class)
public abstract class PufferfishMixin {

    @Decorate(method = "tick()V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$initPuffChange(@Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        allowed = true;
        DecorationOps.blackhole().invoke(allowed);
    }

    // Dispatch at the selected transition, before either its sound or its state write.
    @Decorate(method = "tick()V", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;makeSound(Lnet/minecraft/sounds/SoundEvent;)V"), require = 1, allow = 1)
    private void arclight$inflateSmall(Pufferfish fish, SoundEvent sound,
                                     @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        allowed = new PufferFishStateChangeEvent((PufferFish) ((LivingEntityBridge) fish).bridge$getBukkitEntity(), Pufferfish.STATE_MID).callEvent();
        DecorationOps.blackhole().invoke(allowed);
        if (allowed) {
            DecorationOps.callsite().invoke(fish, sound);
        }
    }

    @Decorate(method = "tick()V", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;makeSound(Lnet/minecraft/sounds/SoundEvent;)V"), require = 1, allow = 1)
    private void arclight$inflateMid(Pufferfish fish, SoundEvent sound,
                                   @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        allowed = new PufferFishStateChangeEvent((PufferFish) ((LivingEntityBridge) fish).bridge$getBukkitEntity(), Pufferfish.STATE_FULL).callEvent();
        DecorationOps.blackhole().invoke(allowed);
        if (allowed) {
            DecorationOps.callsite().invoke(fish, sound);
        }
    }

    @Decorate(method = "tick()V", at = @At(value = "INVOKE", ordinal = 2,
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;makeSound(Lnet/minecraft/sounds/SoundEvent;)V"), require = 1, allow = 1)
    private void arclight$deflateFull(Pufferfish fish, SoundEvent sound,
                                    @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        allowed = new PufferFishStateChangeEvent((PufferFish) ((LivingEntityBridge) fish).bridge$getBukkitEntity(), Pufferfish.STATE_MID).callEvent();
        DecorationOps.blackhole().invoke(allowed);
        if (allowed) {
            DecorationOps.callsite().invoke(fish, sound);
        }
    }

    @Decorate(method = "tick()V", at = @At(value = "INVOKE", ordinal = 3,
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;makeSound(Lnet/minecraft/sounds/SoundEvent;)V"), require = 1, allow = 1)
    private void arclight$deflateMid(Pufferfish fish, SoundEvent sound,
                                   @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        allowed = new PufferFishStateChangeEvent((PufferFish) ((LivingEntityBridge) fish).bridge$getBukkitEntity(), Pufferfish.STATE_SMALL).callEvent();
        DecorationOps.blackhole().invoke(allowed);
        if (allowed) {
            DecorationOps.callsite().invoke(fish, sound);
        }
    }

    @Decorate(method = "tick()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;setPuffState(I)V"), require = 4, allow = 4)
    private void arclight$applyPuffState(Pufferfish fish, int state,
                                       @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        if (allowed) {
            DecorationOps.callsite().invoke(fish, state);
        }
    }

    // Paper freezes the corresponding timer on veto, but still runs super.tick().
    @Decorate(method = "tick()V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;inflateCounter:I"), require = 1, allow = 1)
    private void arclight$advanceInflation(Pufferfish fish, int counter,
                                         @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        if (allowed) {
            DecorationOps.callsite().invoke(fish, counter);
        }
    }

    @Decorate(method = "tick()V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;deflateTimer:I"), require = 1, allow = 1)
    private void arclight$advanceDeflation(Pufferfish fish, int timer,
                                         @Local(allocate = "arclightPuffAllowed") boolean allowed) throws Throwable {
        if (allowed) {
            DecorationOps.callsite().invoke(fish, timer);
        }
    }

    @Inject(method = "touch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$attack(ServerLevel level, Mob mobEntity, CallbackInfo ci) {
        ((MobBridge) mobEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$collide(Player entityIn, CallbackInfo ci) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
