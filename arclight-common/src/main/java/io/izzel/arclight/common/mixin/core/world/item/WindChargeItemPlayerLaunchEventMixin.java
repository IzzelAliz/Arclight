package io.izzel.arclight.common.mixin.core.world.item;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

@Mixin(WindChargeItem.class)
public abstract class WindChargeItemPlayerLaunchEventMixin {
    // Delay both the native RNG operation and sound operation until successful admission.
    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"), require = 1, allow = 1)
    private float arclight$deferLaunchPitch(RandomSource random, Operation<Float> original,
                                           Level level, Player player, InteractionHand hand,
                                           @Share("arclightItemLaunchPitch") LocalRef<Supplier<Float>> pitch) {
        if (!(level instanceof ServerLevel)) {
            return original.call(random);
        }
        pitch.set(() -> original.call(random));
        return 0.0F;
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), require = 1, allow = 1)
    private void arclight$deferLaunchSound(Level level, Entity excluded, double x, double y, double z,
                                          SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch,
                                          Operation<Void> original,
                                          Level hostLevel, Player player, InteractionHand hand,
                                          @Share("arclightItemLaunchPitch") LocalRef<Supplier<Float>> random,
                                          @Share("arclightItemLaunchSound") LocalRef<Runnable> sound) {
        if (!(level instanceof ServerLevel)) {
            original.call(level, excluded, x, y, z, soundEvent, soundSource, volume, pitch);
            return;
        }
        // The fixed native callers all use this expression. Keep the supplied RNG/sound operations.
        sound.set(() -> original.call(level, excluded, player.getX(), player.getY(), player.getZ(), soundEvent, soundSource, volume,
            0.4F / (random.get().get() * 0.4F + 0.8F)));
    }

    // The factory binds the exact projectile; the admission hook runs after native shooting.
    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"), require = 1, allow = 1)
    private Projectile arclight$launchProjectile(Projectile.ProjectileFactory<? extends Projectile> factory,
                                                ServerLevel serverLevel, ItemStack stack, LivingEntity source,
                                                float offset, float power, float uncertainty, Operation<Projectile> original,
                                                Level level, Player player, InteractionHand hand,
                                                @Share("arclightItemLaunchEvent") LocalRef<PlayerLaunchProjectileEvent> event,
                                                @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        Projectile projectile;
        var createdProjectile = new org.apache.commons.lang3.mutable.MutableObject<Projectile>();
        try {
            projectile = original.call((Projectile.ProjectileFactory<Projectile>) (spawnLevel, shooter, item) -> {
            Projectile created = factory.create(spawnLevel, shooter, item);
            createdProjectile.setValue(created);
            ArclightEventFactory.prepareProjectileLaunch(created, player, stack, event::set);
            return created;
            }, serverLevel, stack, source, offset, power, uncertainty);
        } finally {
            if (createdProjectile.getValue() != null) {
                ((io.izzel.arclight.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge) createdProjectile.getValue()).bridge$setLaunchEvent(null);
            }
        }
        if (!ArclightEventFactory.wasProjectileAccepted(projectile)) {
            ArclightEventFactory.resyncProjectileItem(player, hand);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundCooldownPacket(player.getCooldowns().getCooldownGroup(stack), 0));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
        return projectile;
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;)V"), require = 1, allow = 1)
    private void arclight$launchStatistic(Player player, Stat<?> stat, Operation<Void> original,
                                         @Share("arclightItemLaunchEvent") LocalRef<PlayerLaunchProjectileEvent> event,
                                         @Share("arclightItemLaunchStat") LocalRef<Runnable> award) {
        if (event.get() == null) {
            original.call(player, stat);
        } else {
            award.set(() -> original.call(player, stat));
        }
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"), require = 1, allow = 1)
    private void arclight$finishLaunch(ItemStack stack, int amount, LivingEntity source, Operation<Void> original,
                                      Level level, Player player, InteractionHand hand,
                                      @Share("arclightItemLaunchEvent") LocalRef<PlayerLaunchProjectileEvent> event,
                                      @Share("arclightItemLaunchStat") LocalRef<Runnable> award,
                                      @Share("arclightItemLaunchSound") LocalRef<Runnable> sound) {
        PlayerLaunchProjectileEvent launch = event.get();
        if (launch == null) {
            original.call(stack, amount, source);
            return;
        }
        award.get().run();
        if (launch.shouldConsume()) {
            original.call(stack, amount, source);
        } else if (!player.hasInfiniteMaterials()) {
            ArclightEventFactory.resyncProjectileItem(player, hand);
        }
        sound.get().run();
    }
}
