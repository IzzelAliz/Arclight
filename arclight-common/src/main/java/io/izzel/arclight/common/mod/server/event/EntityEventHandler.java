package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.server.world.item.EntityDropContainer;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EntityEventHandler {

    /**
     * @return whether the event is cancelled. If so no drop will be spawned in dropAllDeathLoot
     */
    public static boolean monitorLivingDrops(LivingEntity living, DamageSource source, List<ItemEntity> drops, boolean isCancelled) {
        if (!(living instanceof LivingEntityBridge bridge)) {
            return false;
        }
        if (source == null) {
            source = living.damageSources().genericKill();
        }
        if (isCancelled) {
            drops.clear();
        }
        if (living instanceof ServerPlayer player) {
            String dmsgOrig = player.getCombatTracker().getDeathMessage().getString();
            Inventory inv = ArclightCaptures.getDeathPlayerInv();
            boolean keepInventory = true;
            if (inv != null) {
                keepInventory = false;
                player.getInventory().replaceWith(inv);
            }
            int expReward = bridge.bridge$getExpReward(source.getEntity());
            final PlayerDeathEvent event;
            try (final var container = new EntityDropContainer()) {
                // Arclight: Spigot drops obtained from getCapturedDrops()
                // Already respect vanilla behaviours by using entity capture
                List<org.bukkit.inventory.ItemStack> loot = container.initDecorate(drops);
                event = ArclightEventFactory.callPlayerDeathEvent(player, source, loot, expReward, dmsgOrig, keepInventory);
                container.convert(loot, drops, bridge::arclight$spawnAtLocationNoAdd);
            }
            if (inv != null) {
                player.getInventory().clearContent();
            }
            ((ServerPlayerEntityBridge) player).arclight$readDeathEvent(event);
        } else {
            final var extra = ArclightCaptures.consumeExtraDrops();
            if (extra != null) {
                drops.addAll(extra);
            }
            final EntityDeathEvent event;
            try (final var container = new EntityDropContainer()) {
                List<ItemStack> itemStackList = container.initDecorate(drops);
                event = ArclightEventFactory.callEntityDeathEvent(living, source, itemStackList);
                container.convert(itemStackList, drops, ((EntityBridge) living)::arclight$spawnAtLocationNoAdd);
            }
            bridge.bridge$setExpToDrop(event.getDroppedExp());
        }
        return drops.isEmpty();
    }
}
