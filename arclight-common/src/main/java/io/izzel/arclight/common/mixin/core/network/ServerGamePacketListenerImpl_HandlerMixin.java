package io.izzel.arclight.common.mixin.core.network;

import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightInventoryHelper;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.stream.Collectors;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_HandlerMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void arclight$playerInteractEvent(ServerboundInteractPacket packet, CallbackInfo ci) {
        Entity entity = this.player.level().getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        InteractionHand hand = packet.hand();
        Vec3 interactVec = packet.location();
        PlayerInteractEntityEvent event;
        if (interactVec != null) {
            event = new PlayerInteractAtEntityEvent((Player) ((ServerPlayerBridge) this.player).bridge$getBukkitEntity(),
                ((EntityBridge) entity).bridge$getBukkitEntity(),
                new org.bukkit.util.Vector(interactVec.x, interactVec.y, interactVec.z),
                hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
        } else {
            event = new PlayerInteractEntityEvent((Player) ((ServerPlayerBridge) this.player).bridge$getBukkitEntity(),
                ((EntityBridge) entity).bridge$getBukkitEntity(),
                hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
        }
        ItemStack itemInHand = this.player.getItemInHand(hand);
        boolean triggerLeashUpdate = !itemInHand.isEmpty() && itemInHand.getItem() == Items.LEAD && entity instanceof Mob;
        Item origItem = ArclightInventoryHelper.getSelectedItemType(this.player.getInventory());

        Bukkit.getPluginManager().callEvent(event);

        if ((entity instanceof Bucketable && entity instanceof LivingEntity && origItem != null && origItem.asItem() == Items.WATER_BUCKET)
            && (event.isCancelled() || ArclightInventoryHelper.getSelectedItem(this.player.getInventory()).isEmpty()
            || ArclightInventoryHelper.getSelectedItem(this.player.getInventory()).getItem() != origItem)) {
            ((EntityBridge) entity).bridge$getBukkitEntity().update(this.player);
            this.player.containerMenu.sendAllDataToRemote();
        }

        if (triggerLeashUpdate && (event.isCancelled() || ArclightInventoryHelper.getSelectedItem(this.player.getInventory()).isEmpty()
            || ArclightInventoryHelper.getSelectedItem(this.player.getInventory()).getItem() != origItem)) {
            this.player.connection.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
        }

        if (event.isCancelled() || ArclightInventoryHelper.getSelectedItem(this.player.getInventory()).isEmpty()
            || ArclightInventoryHelper.getSelectedItem(this.player.getInventory()).getItem() != origItem) {
            ((SynchedEntityDataBridge) entity.getEntityData()).bridge$refresh(this.player);
            if (entity instanceof Allay) {
                this.player.connection.send(new ClientboundSetEquipmentPacket(entity.getId(), Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values())
                    .map(slot -> Pair.of(slot, ((LivingEntity) entity).getItemBySlot(slot).copy())).collect(Collectors.toList())));
                this.player.containerMenu.sendAllDataToRemote();
            }
        }

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAttack", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerPlayer;attack(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$sendDirty(CallbackInfo ci) {
        ItemStack itemstack = this.player.getMainHandItem();
        if (!itemstack.isEmpty() && itemstack.getCount() <= -1) {
            this.player.containerMenu.sendAllDataToRemote();
        }
    }
}
