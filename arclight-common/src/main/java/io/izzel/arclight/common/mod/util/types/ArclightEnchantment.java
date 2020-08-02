package io.izzel.arclight.common.mod.util.types;

import net.minecraft.enchantment.Enchantment;
import org.bukkit.craftbukkit.v.enchantments.CraftEnchantment;
import org.jetbrains.annotations.NotNull;

public class ArclightEnchantment extends CraftEnchantment {

    private final String name;

    public ArclightEnchantment(Enchantment target, String name) {
        super(target);
        this.name = name;
    }

    @Override
    public @NotNull String getName() {
        String name = super.getName();
        if (name.startsWith("UNKNOWN_ENCHANT_")) {
            return this.name;
        } else {
            return name;
        }
    }
}
