package io.izzel.arclight.common.mixin.core.world.item;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThrowablePotionItem.class)
public abstract class ThrowablePotionItemPlayerLaunchEventMixin {

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
            ArclightEventFactory.failedProjectileLaunch(player, hand, stack, projectile, false);
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
                                      @Share("arclightItemLaunchStat") LocalRef<Runnable> award) {
        PlayerLaunchProjectileEvent launch = event.get();
        if (launch == null) {
            original.call(stack, amount, source);
            return;
        }
        if (launch.shouldConsume()) {
            original.call(stack, amount, source);
        } else {
            ArclightEventFactory.resyncProjectileItem(player, hand);
        }
        award.get().run();
    }
}
