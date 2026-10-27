package io.izzel.arclight.common.mixin.core.world.item;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only block placement. Elytra boosting retains its separate PlayerElytraBoostEvent producer. */
@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemPlayerLaunchEventMixin {

    @WrapOperation(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/projectile/Projectile;"), require = 1, allow = 1)
    private Projectile arclight$placedFireworkLaunch(Projectile projectile, ServerLevel level, ItemStack stack,
                                                    Operation<Projectile> original, UseOnContext context,
                                                    @Share("arclightPlacedFireworkLaunch") LocalRef<PlayerLaunchProjectileEvent> event,
                                                    @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null) {
            // Native non-player context has no PlayerEvent. Preserve that path without inventing a shooter.
            return original.call(projectile, level, stack);
        }
        ArclightEventFactory.prepareProjectileLaunch(projectile, player, stack, event::set);
        Projectile spawned;
        try {
            spawned = original.call(projectile, level, stack);
        } finally {
            ((io.izzel.arclight.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge) projectile).bridge$setLaunchEvent(null);
        }
        if (!ArclightEventFactory.wasProjectileAccepted(spawned)) {
            ArclightEventFactory.resyncProjectileItem(player, context.getHand());
            cir.setReturnValue(InteractionResult.PASS);
        }
        return spawned;
    }

    @WrapOperation(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), require = 1, allow = 1)
    private void arclight$consumePlacedFirework(ItemStack stack, int amount, Operation<Void> original,
                                               UseOnContext context,
                                               @Share("arclightPlacedFireworkLaunch") LocalRef<PlayerLaunchProjectileEvent> event) {
        Player player = context.getPlayer();
        PlayerLaunchProjectileEvent launch = event.get();
        if (player == null || launch == null) {
            original.call(stack, amount);
        } else if (launch.shouldConsume() && !player.hasInfiniteMaterials()) {
            original.call(stack, amount);
        } else {
            ArclightEventFactory.resyncProjectileItem(player, context.getHand());
        }
    }
}
