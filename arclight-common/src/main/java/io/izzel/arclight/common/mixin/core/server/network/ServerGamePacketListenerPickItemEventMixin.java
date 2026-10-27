package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerPickItemEventMixin {

    @Shadow public ServerPlayer player;

    @Unique private Pair<Object, Boolean> arclight$pickItemContext;

    @Decorate(method = "handlePickItemFromBlock(Lnet/minecraft/network/protocol/game/ServerboundPickItemFromBlockPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryPickItem(Lnet/minecraft/world/item/ItemStack;)V"),
        require = 1, allow = 1)
    private void arclight$pickBlockContext(ServerGamePacketListenerImpl listener, ItemStack stack,
                                         @Local(ordinal = 0) ServerboundPickItemFromBlockPacket packet) throws Throwable {
        Pair<Object, Boolean> previous = this.arclight$pickItemContext;
        this.arclight$pickItemContext = Pair.of(packet.pos(), packet.includeData());
        try {
            DecorationOps.callsite().invoke(listener, stack);
        } finally {
            this.arclight$pickItemContext = previous;
        }
    }

    @Decorate(method = "handlePickItemFromEntity(Lnet/minecraft/network/protocol/game/ServerboundPickItemFromEntityPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryPickItem(Lnet/minecraft/world/item/ItemStack;)V"),
        require = 1, allow = 1)
    private void arclight$pickEntityContext(ServerGamePacketListenerImpl listener, ItemStack stack,
                                          @Local(ordinal = 0) ServerboundPickItemFromEntityPacket packet,
                                          @Local(ordinal = 0) Entity entity) throws Throwable {
        Pair<Object, Boolean> previous = this.arclight$pickItemContext;
        this.arclight$pickItemContext = Pair.of(entity, packet.includeData());
        try {
            DecorationOps.callsite().invoke(listener, stack);
        } finally {
            this.arclight$pickItemContext = previous;
        }
    }

    @Decorate(method = "tryPickItem(Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"),
        require = 1, allow = 1)
    private int arclight$pickItemEvent(Inventory inventory, ItemStack stack,
                                     @Local(allocate = "arclightPickItemEvent") PlayerPickItemEvent event) throws Throwable {
        int sourceSlot = (int) DecorationOps.callsite().invoke(inventory, stack);
        event = null;
        Pair<Object, Boolean> context = this.arclight$pickItemContext;
        if (context != null) {
            int targetSlot = Inventory.isHotbarSlot(sourceSlot) ? sourceSlot : inventory.getSuitableHotbarSlot();
            var bukkitPlayer = ((ServerPlayerBridge) this.player).bridge$getBukkitEntity();
            event = context.getFirst() instanceof BlockPos
                ? new PlayerPickBlockEvent(bukkitPlayer, CraftBlock.at(this.player.level(), (BlockPos) context.getFirst()), context.getSecond(), targetSlot, sourceSlot)
                : new PlayerPickEntityEvent(bukkitPlayer, ((EntityBridge) context.getFirst()).bridge$getBukkitEntity(), context.getSecond(), targetSlot, sourceSlot);
            if (!event.callEvent()) {
                // Return from tryPickItem, not from the packet handler: the native avatar
                // profile/permission tail remains independent of the inventory action.
                return (int) DecorationOps.cancel().invoke();
            }
            sourceSlot = event.getSourceSlot();
        }
        DecorationOps.blackhole().invoke(event);
        return sourceSlot;
    }

    @Decorate(method = "tryPickItem(Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"),
        require = 1, allow = 1)
    private void arclight$pickHotbarTarget(Inventory inventory, int sourceSlot,
                                         @Local(allocate = "arclightPickItemEvent") PlayerPickItemEvent event) throws Throwable {
        int targetSlot = event == null ? sourceSlot : event.getTargetSlot();
        DecorationOps.callsite().invoke(inventory, targetSlot);
    }

    @Decorate(method = "tryPickItem(Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;pickSlot(I)V"),
        require = 1, allow = 1)
    private void arclight$pickInventoryTarget(Inventory inventory, int sourceSlot,
                                            @Local(allocate = "arclightPickItemEvent") PlayerPickItemEvent event) throws Throwable {
        // Keep the native swap (and any preceding mod wrapper). Only its hotbar choice changes.
        int targetSlot = event == null ? -1 : event.getTargetSlot();
        var previous = ArclightCaptures.pickItemTarget;
        ArclightCaptures.pickItemTarget = Pair.of(inventory, targetSlot);
        try {
            DecorationOps.callsite().invoke(inventory, sourceSlot);
        } finally {
            ArclightCaptures.pickItemTarget = previous;
        }
    }

    @Decorate(method = "tryPickItem(Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;addAndPickItem(Lnet/minecraft/world/item/ItemStack;)V"),
        require = 1, allow = 1)
    private void arclight$pickCreativeTarget(Inventory inventory, ItemStack stack,
                                          @Local(allocate = "arclightPickItemEvent") PlayerPickItemEvent event) throws Throwable {
        // The native hasInfiniteMaterials branch still owns item creation.
        int targetSlot = event == null ? -1 : event.getTargetSlot();
        var previous = ArclightCaptures.pickItemTarget;
        ArclightCaptures.pickItemTarget = Pair.of(inventory, targetSlot);
        try {
            DecorationOps.callsite().invoke(inventory, stack);
        } finally {
            ArclightCaptures.pickItemTarget = previous;
        }
    }

    @Decorate(method = "tryPickItem(Lnet/minecraft/world/item/ItemStack;)V", inject = true,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/inventory/InventoryMenu;broadcastChanges()V"),
        require = 1, allow = 1)
    private void arclight$pickEquipmentUpdate() {
        if (GlobalConfiguration.get().unsupportedSettings.updateEquipmentOnPlayerActions) {
            this.player.detectEquipmentUpdates();
        }
    }
}
