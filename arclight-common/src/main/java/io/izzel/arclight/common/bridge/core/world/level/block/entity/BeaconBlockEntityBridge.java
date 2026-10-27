package io.izzel.arclight.common.bridge.core.world.level.block.entity;

import org.bukkit.potion.PotionEffect;

public interface BeaconBlockEntityBridge {

    PotionEffect bridge$getPrimaryEffect();

    PotionEffect bridge$getSecondaryEffect();

    default org.bukkit.potion.PotionEffect getPrimaryEffect() {
        return this.bridge$getPrimaryEffect();
    }

    default org.bukkit.potion.PotionEffect getSecondaryEffect() {
        return this.bridge$getSecondaryEffect();
    }

}
