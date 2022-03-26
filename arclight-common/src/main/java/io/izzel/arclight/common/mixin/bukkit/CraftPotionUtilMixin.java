package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import org.bukkit.craftbukkit.v.potion.CraftPotionUtil;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftPotionUtil.class, remap = false)
public class CraftPotionUtilMixin {

    @Shadow @Final private static BiMap<PotionType, String> upgradeable;
    @Shadow @Final private static BiMap<PotionType, String> extendable;
    @Shadow @Final @Mutable private static BiMap<PotionType, String> regular;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static String fromBukkit(PotionData data) {
        String type;
        if (data.isUpgraded()) {
            type = upgradeable.get(data.getType());
        } else if (data.isExtended()) {
            type = extendable.get(data.getType());
        } else {
            type = regular.get(data.getType());
        }

        Preconditions.checkNotNull(type, "Unknown potion type from data " + data);
        if (type.indexOf(':') != -1) {
            return type;
        } else {
            return "minecraft:" + type;
        }
    }
}
