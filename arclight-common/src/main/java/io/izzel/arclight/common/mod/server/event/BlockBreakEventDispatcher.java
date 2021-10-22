package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

public class BlockBreakEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (DistValidate.isValid(event.getWorld())) {
            CraftBlock craftBlock = CraftBlock.at(event.getWorld(), event.getPos());
            BlockBreakEvent breakEvent = new BlockBreakEvent(craftBlock, ((ServerPlayerEntityBridge) event.getPlayer()).bridge$getBukkitEntity());
            ArclightCaptures.captureBlockBreakPlayer(breakEvent);
            breakEvent.setCancelled(event.isCanceled());
            breakEvent.setExpToDrop(event.getExpToDrop());
            Bukkit.getPluginManager().callEvent(breakEvent);
            event.setCanceled(breakEvent.isCancelled());
            event.setExpToDrop(breakEvent.getExpToDrop());
        }
    }

    @SubscribeEvent
    public void onFarmlandBreak(BlockEvent.FarmlandTrampleEvent event) {
        if (!DistValidate.isValid(event.getWorld())) return;
        Entity entity = event.getEntity();
        Cancellable cancellable;
        if (entity instanceof Player) {
            cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, Action.PHYSICAL, event.getPos(), null, null, null);
        } else {
            cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(event.getWorld(), event.getPos()));
            Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if (cancellable.isCancelled()) {
            event.setCanceled(true);
            return;
        }

        if (CraftEventFactory.callEntityChangeBlockEvent(entity, event.getPos(), Blocks.DIRT.defaultBlockState()).isCancelled()) {
            event.setCanceled(true);
        }
    }
}
