package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.function.Supplier;

@Mixin({SplashPotionItem.class, LingeringPotionItem.class})
public abstract class ThrowablePotionSoundPlayerLaunchEventMixin {

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"), require = 1, allow = 1)
    private float arclight$deferPotionPitch(RandomSource random, Operation<Float> original,
                                           Level level, Player player, InteractionHand hand,
                                           @Share("arclightPotionLaunchPitch") LocalRef<Supplier<Float>> pitch) {
        if (!(level instanceof ServerLevel)) {
            return original.call(random);
        }
        pitch.set(() -> original.call(random));
        return 0.0F;
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), require = 1, allow = 1)
    private void arclight$deferPotionSound(Level level, Entity excluded, double x, double y, double z,
                                          SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch,
                                          Operation<Void> original,
                                          Level hostLevel, Player player, InteractionHand hand,
                                          @Share("arclightPotionLaunchPitch") LocalRef<Supplier<Float>> random,
                                          @Share("arclightPotionLaunchSound") LocalRef<Runnable> sound) {
        if (!(level instanceof ServerLevel)) {
            original.call(level, excluded, x, y, z, soundEvent, soundSource, volume, pitch);
            return;
        }
        sound.set(() -> original.call(level, excluded, player.getX(), player.getY(), player.getZ(), soundEvent, soundSource, volume,
            0.4F / (random.get().get() * 0.4F + 0.8F)));
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ThrowablePotionItem;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"), require = 1, allow = 1)
    private InteractionResult arclight$potionSoundAfterLaunch(@Coerce Object item, Level level, Player player,
                                                             InteractionHand hand, Operation<InteractionResult> original,
                                                             @Share("arclightPotionLaunchSound") LocalRef<Runnable> sound) {
        InteractionResult result = original.call(item, level, player, hand);
        if (!(result instanceof InteractionResult.Fail) && sound.get() != null) {
            sound.get().run();
        }
        return result;
    }
}
