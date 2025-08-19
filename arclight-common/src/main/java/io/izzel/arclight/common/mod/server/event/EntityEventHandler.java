package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.mod.server.world.item.ArclightItemStack;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EntityEventHandler {
    /**
     * @return whether the event is cancelled. If so no drop will be spawned in dropAllDeathLoot
     */
    public static boolean monitorLivingDrops(LivingEntity living, DamageSource source, List<ItemEntity> drops, boolean isCancelled) {
        if (living instanceof ServerPlayer) {
            // capture item entities for handler at ServerPlayerEntityMixin#onDeath
            if (!isCancelled) {
                ArclightCaptures.captureExtraDrops(drops);
            }
            return true;
        } else {
            if (source == null) {
                source = living.damageSources().genericKill();
            }
            if (isCancelled) {
                drops.clear();
            }
            final var extra = ArclightCaptures.consumeExtraDrops();
            if (extra != null) {
                drops.addAll(extra);
            }

            try {
                List<ItemStack> itemStackList = ArclightItemStack.initDecorate(drops);
                final var event = ArclightEventFactory.callEntityDeathEvent(living, source, itemStackList);
                ArclightItemStack.convert(itemStackList, drops, it -> ((EntityBridge) living).arclight$spawnAtLocationNoAdd(it, 0f));
                ((LivingEntityBridge) living).bridge$setExpToDrop(event.getDroppedExp());
            } finally {
                ArclightItemStack.cleanup();
            }
            return drops.isEmpty();
        }
    }
}
