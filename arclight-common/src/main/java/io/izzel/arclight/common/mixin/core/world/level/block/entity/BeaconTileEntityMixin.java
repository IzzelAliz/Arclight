package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.tileentity.BeaconTileEntityBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.bukkit.craftbukkit.v.potion.CraftPotionUtil;
import org.bukkit.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconTileEntityMixin implements BeaconTileEntityBridge {

    // @formatter:off
    @Shadow @Nullable public MobEffect primaryPower;
    @Shadow public int levels;
    @Shadow @Nullable public MobEffect secondaryPower;
    // @formatter:on

    @Inject(method = "load", at = @At("RETURN"))
    public void arclight$level(CompoundTag compound, CallbackInfo ci) {
        this.levels = compound.getInt("Levels");
    }

    public PotionEffect getPrimaryEffect() {
        return (this.primaryPower != null) ? CraftPotionUtil.toBukkit(new MobEffectInstance(this.primaryPower, this.getLevel(), this.getAmplification(), true, true)) : null;
    }

    public PotionEffect getSecondaryEffect() {
        return (this.hasSecondaryEffect()) ? CraftPotionUtil.toBukkit(new MobEffectInstance(this.secondaryPower, getLevel(), getAmplification(), true, true)) : null;
    }

    private byte getAmplification() {
        byte b0 = 0;
        if (this.levels >= 4 && this.primaryPower == this.secondaryPower) {
            b0 = 1;
        }
        return b0;
    }

    private int getLevel() {
        int i = (9 + this.levels * 2) * 20;
        return i;
    }

    private boolean hasSecondaryEffect() {
        if (this.levels >= 4 && this.primaryPower != this.secondaryPower && this.secondaryPower != null) {
            return true;
        }
        return false;
    }

    @Override
    public PotionEffect bridge$getPrimaryEffect() {
        return getPrimaryEffect();
    }

    @Override
    public PotionEffect bridge$getSecondaryEffect() {
        return getSecondaryEffect();
    }
}
