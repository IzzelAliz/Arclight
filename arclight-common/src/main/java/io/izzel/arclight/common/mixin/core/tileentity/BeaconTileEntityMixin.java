package io.izzel.arclight.common.mixin.core.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.BeaconTileEntity;
import org.bukkit.craftbukkit.v.potion.CraftPotionUtil;
import org.bukkit.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.common.bridge.tileentity.BeaconTileEntityBridge;

import javax.annotation.Nullable;

@Mixin(BeaconTileEntity.class)
public abstract class BeaconTileEntityMixin implements BeaconTileEntityBridge {

    // @formatter:off
    @Shadow @Nullable public Effect primaryEffect;
    @Shadow public int levels;
    @Shadow @Nullable public Effect secondaryEffect;
    // @formatter:on

    @Inject(method = "read", at = @At("RETURN"))
    public void arclight$level(BlockState state, CompoundNBT compound, CallbackInfo ci) {
        this.levels = compound.getInt("Levels");
    }

    public PotionEffect getPrimaryEffect() {
        return (this.primaryEffect != null) ? CraftPotionUtil.toBukkit(new EffectInstance(this.primaryEffect, this.getLevel(), this.getAmplification(), true, true)) : null;
    }

    public PotionEffect getSecondaryEffect() {
        return (this.hasSecondaryEffect()) ? CraftPotionUtil.toBukkit(new EffectInstance(this.secondaryEffect, getLevel(), getAmplification(), true, true)) : null;
    }

    private byte getAmplification() {
        byte b0 = 0;
        if (this.levels >= 4 && this.primaryEffect == this.secondaryEffect) {
            b0 = 1;
        }
        return b0;
    }

    private int getLevel() {
        int i = (9 + this.levels * 2) * 20;
        return i;
    }

    private boolean hasSecondaryEffect() {
        if (this.levels >= 4 && this.primaryEffect != this.secondaryEffect && this.secondaryEffect != null) {
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
