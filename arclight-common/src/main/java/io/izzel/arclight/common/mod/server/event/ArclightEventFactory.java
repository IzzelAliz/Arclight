package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import io.izzel.arclight.common.bridge.inject.InjectEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.damage.CraftDamageSource;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public abstract class ArclightEventFactory {

    public static<T extends Event> T callEvent(T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * @see CraftEventFactory#callEntityDeathEvent
     */
    public static EntityDeathEvent callEntityDeathEvent(LivingEntity entity, DamageSource damageSource, List<ItemStack> drops) {
        CraftDamageSource bukkitDamageSource = new CraftDamageSource(damageSource);
        CraftLivingEntity craftLivingEntity = ((LivingEntityBridge) entity).bridge$getBukkitEntity();
        EntityDeathEvent event = new EntityDeathEvent(craftLivingEntity, bukkitDamageSource, drops, ((LivingEntityBridge) entity).bridge$getExpReward(damageSource.getEntity()));
        return callEvent(event);
    }

    /**
     * @see CraftEventFactory#callPlayerDeathEvent(ServerPlayer, DamageSource, List, String, boolean)
     */
    public static PlayerDeathEvent callPlayerDeathEvent(ServerPlayer victim, DamageSource damageSource, List<ItemStack> drops, int expReward, String deathMessage, boolean keepInventory) {
        CraftPlayer entity = (CraftPlayer) victim.bridge$getBukkitEntity();
        CraftDamageSource bukkitDamageSource = new CraftDamageSource(damageSource);
        PlayerDeathEvent event = new PlayerDeathEvent(entity, bukkitDamageSource, drops, expReward, 0, deathMessage);
        event.setKeepInventory(keepInventory);
        event.setKeepLevel(((ServerPlayerBridge) victim).arclight$isKeepLevel());
        callEvent(event);
        return event;
    }

    /**
     * @see CraftEventFactory#handleBlockFormEvent
     */
    @Nullable
    public static BlockFormEvent callBlockFormEvent(Level world, BlockPos pos, net.minecraft.world.level.block.state.BlockState block, int flag, @Nullable net.minecraft.world.entity.Entity entity) {
        // Suppress during worldgen
        if (!DistValidate.isValid(world)) {
            return null;
        }

        CraftBlockState blockState = CraftBlockStates.getBlockState(world, pos, flag);
        blockState.setData(block);
        BlockFormEvent event = entity == null ? new BlockFormEvent(blockState.getBlock(), blockState) : new EntityBlockFormEvent(entity.bridge$getBukkitEntity(), blockState.getBlock(), blockState);
        return callEvent(event);
    }

    /**
     * @return if we can drop the item
     */
    public static<T extends ItemEntity & InjectEntityBridge> boolean callEntityDropItemEvent(InjectEntityBridge entity, T drop) {
        EntityDropItemEvent bukkitEvent = new EntityDropItemEvent(entity.bridge$getBukkitEntity(), (Item) drop.bridge$getBukkitEntity());
        callEvent(bukkitEvent);
        return !bukkitEvent.isCancelled();
    }

    public static boolean onBlockBreak(ServerPlayerGameMode controller, ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, boolean isSwordNoBreak) {
        // Tell client the block is gone immediately then process events
        // Don't tell the client if it's a creative sword break because it's not broken!
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
        var newPatch = currentStack.getComponentsPatch();

        int size = oldStack.getCount();
        var oldPatch = oldStack.getComponentsPatch();

        currentStack.setCount(size);
        ((ItemStackBridge) (Object) currentStack).arclight$restorePatch(oldPatch);

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
            if (currentStack.getCount() == size && Objects.equals(currentStack.getComponentsPatch(), oldPatch)) {
                ((ItemStackBridge) (Object) currentStack).arclight$restorePatch(newPatch);
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
                block.onPlace(world, newblockposition, oldBlock, true);

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
