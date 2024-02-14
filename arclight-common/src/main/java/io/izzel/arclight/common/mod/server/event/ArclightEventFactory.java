package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public abstract class ArclightEventFactory {

    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static EntityRegainHealthEvent callEntityRegainHealthEvent(Entity entity, float amount, EntityRegainHealthEvent.RegainReason regainReason) {
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(entity, amount, regainReason);
        callEvent(event);
        return event;
    }

    public static EntityResurrectEvent callEntityResurrectEvent(org.bukkit.entity.LivingEntity livingEntity) {
        EntityResurrectEvent event = new EntityResurrectEvent(livingEntity);
        callEvent(event);
        return event;
    }

    public static void callEntityDeathEvent(LivingEntity entity, List<ItemStack> drops) {
        CraftLivingEntity craftLivingEntity = ((LivingEntityBridge) entity).bridge$getBukkitEntity();
        EntityDeathEvent event = new EntityDeathEvent(craftLivingEntity, drops, ((LivingEntityBridge) entity).bridge$getExpReward());
        callEvent(event);
        ((LivingEntityBridge) entity).bridge$setExpToDrop(event.getDroppedExp());
    }

    public static EntityDeathEvent callEntityDeathEvent(org.bukkit.entity.LivingEntity entity, List<ItemStack> drops, int droppedExp) {
        EntityDeathEvent event = new EntityDeathEvent(entity, drops, droppedExp);
        callEvent(event);
        return event;
    }

    public static EntityDropItemEvent callEntityDropItemEvent(org.bukkit.entity.Entity entity, org.bukkit.entity.Item drop) {
        EntityDropItemEvent bukkitEvent = new EntityDropItemEvent(entity, drop);
        callEvent(bukkitEvent);
        return bukkitEvent;
    }

    public static boolean onBlockBreak(ServerPlayerGameMode controller, ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, boolean isSwordNoBreak) {
        // Tell client the block is gone immediately then process events
        // Don't tell the client if its a creative sword break because its not broken!
        if (level.getBlockEntity(pos) == null && !isSwordNoBreak) {
            var packet = new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState());
            player.connection.send(packet);
        }

        var bblock = CraftBlock.at(level, pos);
        var event = new BlockBreakEvent(bblock, (Player) player.bridge$getBukkitEntity());
        ArclightCaptures.captureBlockBreakPlayer(event);

        // Sword + Creative mode pre-cancel
        event.setCancelled(isSwordNoBreak);

        // Calculate default block experience
        var nmsBlock = state.getBlock();

        var itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);

        if (!event.isCancelled() && !controller.isCreative() && player.hasCorrectToolForDrops(nmsBlock.defaultBlockState())) {
            event.setExpToDrop(((BlockBridge) nmsBlock).bridge$getExpDrop(state, level, pos, itemstack));
        }

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            if (isSwordNoBreak) {
                return false;
            }
            // Let the client know the block still exists
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));

            // Brute force all possible updates
            for (var dir : Direction.values()) {
                player.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(dir)));
            }

            // Update any tile entity data for this block
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var packet = blockEntity.getUpdatePacket();
                if (packet != null) {
                    player.connection.send(packet);
                }
            }
            return false;
        }

        // CraftBukkit - update state from plugins
        if (level.getBlockState(pos).isAir()) {
            return false;
        }
        return true;
    }

    public static InteractionResult onBlockPlace(UseOnContext context, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack oldStack, net.minecraft.world.item.ItemStack currentStack, InteractionResult result) {
        var world = (ServerLevel) context.getLevel();
        var blockposition = context.getClickedPos();
        var enumhand = context.getHand();
        org.bukkit.event.block.BlockPlaceEvent placeEvent = null;
        List<org.bukkit.block.BlockState> blocks = new java.util.ArrayList<>(((WorldBridge) world).bridge$getCapturedBlockState().values());

        // save new item data
        int newSize = currentStack.getCount();
        CompoundTag newNBT = null;
        if (currentStack.getTag() != null) {
            newNBT = currentStack.getTag().copy();
        }

        int size = oldStack.getCount();
        CompoundTag nbt = null;
        if (oldStack.getTag() != null) {
            nbt = oldStack.getTag().copy();
        }

        currentStack.setCount(size);
        currentStack.setTag(nbt);

        if (blocks.size() > 1) {
            placeEvent = CraftEventFactory.callBlockMultiPlaceEvent(world, player, enumhand, blocks, blockposition.getX(), blockposition.getY(), blockposition.getZ());
        } else if (blocks.size() == 1) {
            placeEvent = CraftEventFactory.callBlockPlaceEvent(world, player, enumhand, blocks.get(0), blockposition.getX(), blockposition.getY(), blockposition.getZ());
        }

        if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
            result = InteractionResult.FAIL;
            // PAIL: Remove this when MC-99075 fixed
            placeEvent.getPlayer().updateInventory();
            // revert back all captured blocks
            ((WorldBridge) world).bridge$preventPoiUpdated(true); // CraftBukkit - SPIGOT-5710
            for (org.bukkit.block.BlockState blockstate : blocks) {
                blockstate.update(true, false);
            }
            ((WorldBridge) world).bridge$preventPoiUpdated(false);

            // Brute force all possible updates
            var placedPos = ((CraftBlock) placeEvent.getBlock()).getPosition();
            for (var dir : Direction.values()) {
                ((ServerPlayer) player).connection.send(new ClientboundBlockUpdatePacket(world, placedPos.relative(dir)));
            }
            // ItemSign.openSign = null; // SPIGOT-6758 - Reset on early return
        } else {
            // Change the stack to its new contents if it hasn't been tampered with.
            if (currentStack.getCount() == size && Objects.equals(currentStack.getTag(), nbt)) {
                currentStack.setTag(newNBT);
                currentStack.setCount(newSize);
            }

            for (var e : ((WorldBridge) world).bridge$getCapturedBlockEntity().entrySet()) {
                world.setBlockEntity(e.getValue());
            }

            for (var blockstate : blocks) {
                int updateFlag = ((CraftBlockState) blockstate).getFlag();
                var oldBlock = ((CraftBlockState) blockstate).getHandle();
                var newblockposition = ((CraftBlockState) blockstate).getPosition();
                var block = world.getBlockState(newblockposition);
                block.getBlock().onPlace(block, world, newblockposition, oldBlock, true);

                ((WorldBridge) world).bridge$forge$notifyAndUpdatePhysics(newblockposition, null, oldBlock, block, updateFlag, 512); // send null chunk as chunk.k() returns false by this point
            }

            // SPIGOT-7315: Moved from BlockBed#setPlacedBy
            if (placeEvent != null && currentStack.getItem() instanceof BedItem) {
                var position = ((CraftBlock) placeEvent.getBlock()).getPosition();
                var blockData = world.getBlockState(position);

                if (blockData.getBlock() instanceof BedBlock) {
                    world.blockUpdated(position, Blocks.AIR);
                    blockData.updateNeighbourShapes(world, position, 3);
                }
            }
        }
        return result;
    }
}
