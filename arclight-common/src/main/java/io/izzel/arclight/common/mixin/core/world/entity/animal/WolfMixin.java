package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import io.izzel.arclight.common.mixin.core.world.entity.TamableAnimalMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityDyeEvent;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimalMixin {

    @Shadow @Final private static EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    @Decorate(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setCollarColor(Lnet/minecraft/world/item/DyeColor;)V"), require = 1, allow = 1)
    private void arclight$dyeCollar(Wolf wolf, DyeColor color, Player player, InteractionHand hand) throws Throwable {
        var event = new EntityDyeEvent(this.getBukkitEntity(), org.bukkit.DyeColor.getByWoolData((byte) color.getId()),
            (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity());
        if (!event.callEvent()) {
            this.arclight$resyncCollar(wolf, player);
            if (!player.hasInfiniteMaterials()) {
                arclight$resyncCollarDyeItem(player, hand);
            }
            // Keep the native dye consumption out of the cancelled interaction.
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            return;
        }
        DyeColor dyedColor = DyeColor.byId(event.getColor().getWoolData());
        if (color != dyedColor) {
            // Match Paper's pre-setter correction of the client's predicted collar color.
            this.arclight$resyncCollar(wolf, player);
        }
        DecorationOps.callsite().invoke(wolf, dyedColor);
    }

    @Unique
    private void arclight$resyncCollar(Wolf wolf, Player player) {
        if (player instanceof ServerPlayer serverPlayer
            && ((ServerPlayerBridge) serverPlayer).bridge$getBukkitEntity().canSee(this.getBukkitEntity())) {
            serverPlayer.connection.send(new ClientboundSetEntityDataPacket(wolf.getId(),
                List.of(SynchedEntityData.DataValue.create(DATA_COLLAR_COLOR, this.entityData.get(DATA_COLLAR_COLOR)))));
        }
    }

    @Unique
    private static void arclight$resyncCollarDyeItem(Player player, InteractionHand hand) {
        if (player.containerMenu instanceof InventoryMenu) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
            ((AbstractContainerMenuBridge) player.containerMenu).bridge$forceSlot(player.getInventory(), slot);
        } else {
            player.containerMenu.sendAllDataToRemote();
            // Native full-menu synchronization does not include the offhand.
            ((AbstractContainerMenuBridge) player.inventoryMenu).bridge$forceSlot(player.getInventory(), Inventory.SLOT_OFFHAND);
        }
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setOrderedToSit(Z)V"))
    private void arclight$handledBy(Wolf wolfEntity, boolean p_233687_1_) {
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void arclight$resetSitOnDamage(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ((Wolf) (Object) this).setOrderedToSit(false);
        }
    }

    @Inject(method = "applyTamingSideEffects", at = @At("RETURN"))
    private void arclight$healToMax(CallbackInfo ci) {
        if (this.isTame()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$attackReason(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }

    @Redirect(method = "tryToTame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) {
        var ret = instance.nextInt(i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }

    // Force drop handler moved to PSI
}
