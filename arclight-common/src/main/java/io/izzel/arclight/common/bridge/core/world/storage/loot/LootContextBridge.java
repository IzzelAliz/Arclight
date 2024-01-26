package io.izzel.arclight.common.bridge.core.world.storage.loot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public interface LootContextBridge {

    default int bridge$forge$getLootingModifier(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return EnchantmentHelper.getMobLooting(livingEntity);
        } else {
            return 0;
        }
    }
}
