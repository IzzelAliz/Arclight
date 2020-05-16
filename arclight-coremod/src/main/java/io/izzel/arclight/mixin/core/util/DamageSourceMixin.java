package io.izzel.arclight.mixin.core.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.bridge.util.DamageSourceBridge;

import javax.annotation.Nullable;

@Mixin(DamageSource.class)
public abstract class DamageSourceMixin implements DamageSourceBridge {

    // @formatter:off
    @Shadow @Nullable public Entity getTrueSource() { return null; }
    // @formatter:on

    private boolean sweep;

    public boolean isSweep() {
        return sweep;
    }

    @Override
    public boolean bridge$isSweep() {
        return isSweep();
    }

    public DamageSource sweep() {
        sweep = true;
        return (DamageSource) (Object) this;
    }

    @Override
    public DamageSource bridge$sweep() {
        return sweep();
    }
}
