package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.forge.mod.util.ArclightBlockSnapshot;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceEventDispatcher {

    public static boolean onBlockPlace(BlockEvent.EntityPlaceEvent event, boolean cancelled) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerBridge playerEntity) {
            Player player = playerEntity.bridge$getBukkitEntity();
            Direction direction = ArclightCaptures.getPlaceEventDirection();
            if (direction != null && DistValidate.isValid(event.getLevel())) {
                InteractionHand hand = ArclightCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
                CraftBlock placedBlock = ArclightBlockSnapshot.fromBlockSnapshot(event.getBlockSnapshot(), true);
                CraftBlock againstBlock = CraftBlock.at(event.getLevel(), event.getPos().relative(direction.getOpposite()));
                ItemStack bukkitStack;
                EquipmentSlot bukkitHand;
                if (hand == InteractionHand.MAIN_HAND) {
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
                        !cancelled,
                        bukkitHand
                );
                placeEvent.setCancelled(cancelled);
                Bukkit.getPluginManager().callEvent(placeEvent);
                cancelled = placeEvent.isCancelled() || !placeEvent.canBuild();
            }
        }
        return cancelled;
    }

    public static boolean onMultiPlace(BlockEvent.EntityMultiPlaceEvent event, boolean cancelled) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerBridge playerEntity) {
            Player player = playerEntity.bridge$getBukkitEntity();
            Direction direction = ArclightCaptures.getPlaceEventDirection();
            if (direction != null && DistValidate.isValid(event.getLevel())) {
                InteractionHand hand = ArclightCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
                List<BlockState> placedBlocks = new ArrayList<>(event.getReplacedBlockSnapshots().size());
                for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                    placedBlocks.add(ArclightBlockSnapshot.fromBlockSnapshot(snapshot, true).getState());
                }
                CraftBlock againstBlock = CraftBlock.at(event.getLevel(), event.getPos().relative(direction.getOpposite()));
                ItemStack bukkitStack;
                if (hand == InteractionHand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                }
                BlockPlaceEvent placeEvent = new BlockMultiPlaceEvent(
                        placedBlocks,
                        againstBlock,
                        bukkitStack,
                        player,
                        !cancelled
                );
                placeEvent.setCancelled(cancelled);
                Bukkit.getPluginManager().callEvent(placeEvent);
                cancelled = placeEvent.isCancelled() || !placeEvent.canBuild();
            }
        }
        return cancelled;
    }
}
