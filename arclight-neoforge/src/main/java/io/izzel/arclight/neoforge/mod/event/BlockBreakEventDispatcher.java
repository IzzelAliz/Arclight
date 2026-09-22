package io.izzel.arclight.neoforge.mod.event;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakEventDispatcher {

    // todo
    @SubscribeEvent(receiveCanceled = true)
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (DistValidate.isValid(event.getLevel())) {
            CraftBlock craftBlock = CraftBlock.at(event.getLevel(), event.getPos());
            BlockBreakEvent breakEvent = new BlockBreakEvent(craftBlock, ((ServerPlayerBridge) event.getPlayer()).bridge$getBukkitEntity());
            ArclightCaptures.captureBlockBreakPlayer(breakEvent);
            breakEvent.setCancelled(event.isCanceled());
            ServerLevel level = (ServerLevel) event.getLevel();
            BlockEntity blockEntity = level.getBlockEntity(event.getPos());
            ItemStack tool = event.getPlayer().getMainHandItem();
            breakEvent.setExpToDrop(EnchantmentHelper.processBlockExperience(level, tool, event.getState().getExpDrop(level, event.getPos(), blockEntity, event.getPlayer(), tool)));
            Bukkit.getPluginManager().callEvent(breakEvent);
            event.setCanceled(breakEvent.isCancelled());
        }
    }
}
