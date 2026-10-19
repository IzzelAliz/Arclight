package io.izzel.arclight.common.mixin.core.network;

import com.mojang.datafixers.util.Pair;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.Collectors;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_HandlerMixin {

    @Shadow private ServerPlayer player;

    // A per-invocation frame prevents a synchronous Bukkit callback from replacing the outer hand snapshot.
    // The EMPTY frame also accounts for all native early returns before the copy callsite.
    @Unique private final Deque<ItemStack> arclight$interactHandFrames = new ArrayDeque<>();

    // The share belongs to this invocation, not this connection/thread. In particular the
    // wrapper must not touch the deque before the native thread gate has returned.
    @WrapMethod(method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V")
    private void arclight$interactScope(ServerboundInteractPacket packet, Operation<Void> original,
                                        @Share("interactFrameActive") LocalBooleanRef active) {
        try {
            original.call(packet);
        } finally {
            // Normal and DecorationOps.cancel() returns already popped at RETURN. Only
            // an exceptional exit after begin owns a remaining frame; never pop a parent.
            if (active.get()) {
                active.set(false);
                arclight$interactHandFrames.pop();
            }
        }
    }

    // The existing listener's movement/self guard injects at this same point (default order 1000).
    // Run after it so its cancellation cannot strand a hand frame.
    @Inject(method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V", order = 1100, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$beginInteract(ServerboundInteractPacket packet, CallbackInfo ci,
                                        @Share("interactFrameActive") LocalBooleanRef active) {
        arclight$interactHandFrames.push(ItemStack.EMPTY);
        active.set(true);
    }

    // Both vanilla and NeoForge copy the enabled tool exactly once. This callsite is after all
    // vanilla packet checks, but before NeoForge's onInteractEntityAt (including its non-null
    // short circuit). Intercepting interactOn alone would miss mod-handled interactions entirely.
    @Decorate(method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$playerInteractEvent(ItemStack itemInHand,
                                                   @Local(ordinal = -1) Entity entity,
                                                   @Local(ordinal = -1) InteractionHand interactionHand,
                                                   @Local(ordinal = -1) Vec3 location) throws Throwable {
        ServerPlayer player = this.player;
        PlayerInteractAtEntityEvent event = new PlayerInteractAtEntityEvent((Player) player.bridge$getBukkitEntity(), entity.bridge$getBukkitEntity(),
            new org.bukkit.util.Vector(location.x, location.y, location.z), (interactionHand == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
        boolean triggerLeashUpdate = itemInHand.getItem() == Items.LEAD && entity instanceof Mob;
        Item origItem = player.getInventory().getSelectedItem() == null ? null : player.getInventory().getSelectedItem().getItem();

        Bukkit.getPluginManager().callEvent(event);
        boolean changedOrCancelled = event.isCancelled() || player.getInventory().getSelectedItem() == null || player.getInventory().getSelectedItem().getItem() != origItem;
        // Fish bucket - SPIGOT-4048
        if (entity instanceof Bucketable && entity instanceof LivingEntity && origItem != null && origItem.asItem() == Items.WATER_BUCKET && changedOrCancelled) {
            entity.bridge$getBukkitEntity().update(player);
            player.containerMenu.sendAllDataToRemote();
        }
        if (triggerLeashUpdate && changedOrCancelled) {
            player.connection.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
        }
        if (changedOrCancelled) {
            ((SynchedEntityDataBridge) entity.getEntityData()).bridge$refresh(player);
            if (entity instanceof Allay) {
                player.connection.send(new ClientboundSetEquipmentPacket(entity.getId(), Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values()).map((slot) -> Pair.of(slot, ((LivingEntity) entity).getItemBySlot(slot).copy())).collect(Collectors.toList())));
                player.containerMenu.sendAllDataToRemote();
            }
        }
        if (event.isCancelled()) {
            // The synthetic target return still invokes the RETURN cleanup. Keep this frame
            // until then, including during a nested interaction from a Bukkit callback.
            return (ItemStack) DecorationOps.cancel().invoke();
        }
        // Keep the actual pre-event hand object: native interaction (or a NeoForge hook) may
        // mutate it; the copied criterion stack must still come from the original callsite.
        arclight$interactHandFrames.pop();
        arclight$interactHandFrames.push(itemInHand);
        return (ItemStack) DecorationOps.callsite().invoke(itemInHand);
    }

    @Inject(method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V", at = @At("RETURN"))
    private void arclight$finishInteract(ServerboundInteractPacket packet, CallbackInfo ci,
                                         @Share("interactFrameActive") LocalBooleanRef active) {
        if (!active.get()) {
            return;
        }
        ItemStack itemInHand = arclight$interactHandFrames.pop();
        active.set(false);
        if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
            player.containerMenu.sendAllDataToRemote();
        }
    }

    @Decorate(method = "handleAttack(Lnet/minecraft/network/protocol/game/ServerboundAttackPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;attack(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$sendDirty(ServerPlayer player, Entity target) throws Throwable {
        ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
        DecorationOps.callsite().invoke(player, target);
        if (!itemstack.isEmpty() && itemstack.getCount() <= -1) {
            player.containerMenu.sendAllDataToRemote();
        }
    }
}
