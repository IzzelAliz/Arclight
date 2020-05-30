package io.izzel.arclight.common.bridge.tileentity;

import org.bukkit.potion.PotionEffect;

public interface BeaconTileEntityBridge {

    PotionEffect bridge$getPrimaryEffect();

    PotionEffect bridge$getSecondaryEffect();
}
