package io.izzel.arclight.mod.util.potion;

import org.bukkit.Color;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectTypeBuilder extends PotionEffectType {
    private final double durationModifier;
    private final String name;
    private final boolean isInstant;
    private final Color color;

    protected PotionEffectTypeBuilder(int id, double durationModifier, String name, boolean isInstant, Color color) {
        super(id);
        this.durationModifier = durationModifier;
        this.name = name;
        this.isInstant = isInstant;
        this.color = color;
    }

    @Override
    public double getDurationModifier() {
        return durationModifier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isInstant() {
        return isInstant;
    }

    @Override
    public Color getColor() {
        return color;
    }

    public static PotionEffectTypeBuilder build(int id, double durationModifier, String name, boolean isInstant, Color color) {
        return new PotionEffectTypeBuilder(id, durationModifier, name, isInstant, color);
    }
}
