package io.izzel.arclight.common.mixin.core.network;

import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.stream.Collectors;

@Mixin(targets = "net/minecraft/server/network/ServerGamePacketListenerImpl$1")
public class ServerGamePacketListenerImpl_HandlerMixin {

    @Shadow(aliases = {"field_28963", "f_143671_", "this$0"}) private ServerGamePacketListenerImpl outerThis;

    @Unique private transient Vec3 arclight$interactVec;

    @Decorate(method = "performInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl$EntityInteraction;run(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult arclight$playerInteractEvent(ServerGamePacketListenerImpl.EntityInteraction instance, ServerPlayer player, Entity entity, InteractionHand interactionHand) throws Throwable {
        PlayerInteractEntityEvent event;
        if (arclight$interactVec != null) {
            event = new PlayerInteractAtEntityEvent((Player) player.bridge$getBukkitEntity(), entity.bridge$getBukkitEntity(),
                new org.bukkit.util.Vector(arclight$interactVec.x, arclight$interactVec.y, arclight$interactVec.z), (interactionHand == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
        } else {
            event = new PlayerInteractEntityEvent((Player) player.bridge$getBukkitEntity(), entity.bridge$getBukkitEntity(),
                (interactionHand == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
        }
        ItemStack itemInHand = player.getItemInHand(interactionHand);
        boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof Mob;
        Item origItem = player.getInventory().getSelected() == null ? null : player.getInventory().getSelected().getItem();

        Bukkit.getPluginManager().callEvent(event);

        // Fish bucket - SPIGOT-4048
        if ((entity instanceof Bucketable && entity instanceof LivingEntity && origItem != null && origItem.asItem() == Items.WATER_BUCKET) && (event.isCancelled() || player.getInventory().getSelected() == null || player.getInventory().getSelected().getItem() != origItem)) {
            entity.bridge$getBukkitEntity().update(player);
            player.containerMenu.sendAllDataToRemote();
        }

        if (triggerLeashUpdate && (event.isCancelled() || player.getInventory().getSelected() == null || player.getInventory().getSelected().getItem() != origItem)) {
            // Refresh the current leash state
            player.connection.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
        }

        if (event.isCancelled() || player.getInventory().getSelected() == null || player.getInventory().getSelected().getItem() != origItem) {
            // Refresh the current entity metadata
            ((SynchedEntityDataBridge) entity.getEntityData()).bridge$refresh(player);
            if (entity instanceof Allay) {
                player.connection.send(new ClientboundSetEquipmentPacket(entity.getId(), Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values()).map((slot) -> Pair.of(slot, ((LivingEntity) entity).getItemBySlot(slot).copy())).collect(Collectors.toList())));
                player.containerMenu.sendAllDataToRemote();
            }
        }

        if (event.isCancelled()) {
            return (InteractionResult) DecorationOps.cancel().invoke();
        }
        var result = (InteractionResult) DecorationOps.callsite().invoke(instance, player, entity, interactionHand);
        if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
            player.containerMenu.sendAllDataToRemote();
        }
        return result;
    }

    @Inject(method = "onInteraction(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void arclight$setInteractVec(InteractionHand interactionHand, Vec3 vec3, CallbackInfo ci) {
        this.arclight$interactVec = vec3;
    }

    @Inject(method = "onInteraction(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void arclight$resetInteractVec(InteractionHand interactionHand, Vec3 vec3, CallbackInfo ci) {
        this.arclight$interactVec = null;
    }

    @Decorate(method = "onAttack", inject = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerPlayer;attack(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$sendDirty(@Local(ordinal = -1) ItemStack itemstack) {
        if (!itemstack.isEmpty() && itemstack.getCount() <= -1) {
            outerThis.player.containerMenu.sendAllDataToRemote();
        }
    }
}
