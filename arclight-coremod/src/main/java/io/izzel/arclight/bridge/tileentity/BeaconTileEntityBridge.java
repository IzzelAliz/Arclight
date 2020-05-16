package io.izzel.arclight.bridge.tileentity;

import org.bukkit.potion.PotionEffect;

public interface BeaconTileEntityBridge {

    PotionEffect bridge$getPrimaryEffect();

    PotionEffect bridge$getSecondaryEffect();
}
