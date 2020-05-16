package io.izzel.arclight.mod.util.potion;

import net.minecraft.potion.Effect;
import org.bukkit.craftbukkit.v1_14_R1.potion.CraftPotionEffectType;

public class ArclightPotionEffect extends CraftPotionEffectType {

    private final String name;

    public ArclightPotionEffect(Effect handle, String name) {
        super(handle);
        this.name = name;
    }

    @Override
    public String getName() {
        String name = super.getName();
        if (name.startsWith("UNKNOWN_EFFECT_TYPE_")) {
            return this.name;
        } else {
            return name;
        }
    }
}
