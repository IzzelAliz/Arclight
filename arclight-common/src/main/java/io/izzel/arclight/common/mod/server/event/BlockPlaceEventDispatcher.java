package io.izzel.arclight.common.mod.server.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import io.izzel.arclight.common.mod.util.ArclightBlockSnapshot;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerEntity = (ServerPlayerEntity) entity;
            Player player = ((CraftServer) Bukkit.getServer()).getPlayer(playerEntity);
            Direction direction = ArclightCaptures.getPlaceEventDirection();
            if (direction != null) {
                Hand hand = ArclightCaptures.getPlaceEventHand(Hand.MAIN_HAND);
                CraftBlock placedBlock = ArclightBlockSnapshot.fromBlockSnapshot(event.getBlockSnapshot(), true);
                CraftBlock againstBlock = CraftBlock.at(event.getWorld(), event.getPos().offset(direction.getOpposite()));
                ItemStack bukkitStack;
                EquipmentSlot bukkitHand;
                if (hand == Hand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                    bukkitHand = EquipmentSlot.HAND;
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                    bukkitHand = EquipmentSlot.OFF_HAND;
                }
                BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                        placedBlock,
                        placedBlock.getState(),
                        againstBlock,
                        bukkitStack,
                        player,
                        !event.isCanceled(),
                        bukkitHand
                );
                placeEvent.setCancelled(event.isCanceled());
                Bukkit.getPluginManager().callEvent(placeEvent);
                event.setCanceled(placeEvent.isCancelled() || !placeEvent.canBuild());
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerEntity = (ServerPlayerEntity) entity;
            Player player = ((CraftServer) Bukkit.getServer()).getPlayer(playerEntity);
            Direction direction = ArclightCaptures.getPlaceEventDirection();
            if (direction != null) {
                Hand hand = ArclightCaptures.getPlaceEventHand(Hand.MAIN_HAND);
                List<BlockState> placedBlocks = new ArrayList<>(event.getReplacedBlockSnapshots().size());
                for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                    placedBlocks.add(ArclightBlockSnapshot.fromBlockSnapshot(snapshot, true).getState());
                }
                CraftBlock againstBlock = CraftBlock.at(event.getWorld(), event.getPos().offset(direction.getOpposite()));
                ItemStack bukkitStack;
                if (hand == Hand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                }
                BlockPlaceEvent placeEvent = new BlockMultiPlaceEvent(
                        placedBlocks,
                        againstBlock,
                        bukkitStack,
                        player,
                        !event.isCanceled()
                );
                placeEvent.setCancelled(event.isCanceled());
                Bukkit.getPluginManager().callEvent(placeEvent);
                event.setCanceled(placeEvent.isCancelled() || !placeEvent.canBuild());
            }
        }
    }
}
