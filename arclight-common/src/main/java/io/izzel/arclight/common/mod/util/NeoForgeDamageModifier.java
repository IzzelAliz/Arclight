package io.izzel.arclight.common.mod.util;

import java.util.function.Function;

public class NeoForgeDamageModifier {
    private final Function<Double, Double> modifier;
    private final String name;

    public NeoForgeDamageModifier(Function<Double, Double> modifier, String name) {
        this.modifier = modifier;
        this.name = name;
    }

    public double apply(double damage) {
        return modifier.apply(damage);
    }

    public String getName() {
        return name;
    }
} 