package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.tileentity.SculkCatalystListenerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkCatalystBlockEntity.class)
public abstract class SculkCatalystBlockEntityMixin extends BlockEntityMixin {

    @Shadow @Final private SculkCatalystBlockEntity.CatalystListener catalystListener;

    @Override
    public void setLevel(Level p_155231_) {
        super.setLevel(p_155231_);
        ((SculkCatalystListenerBridge) this.catalystListener).bridge$setLevel(p_155231_);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void arclight$overrideSource(Level p_222780_, BlockPos p_222781_, BlockState p_222782_, SculkCatalystBlockEntity blockEntity, CallbackInfo ci) {
        CraftEventFactory.sourceBlockOverride = blockEntity.getBlockPos();
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void arclight$resetSource(Level p_222780_, BlockPos p_222781_, BlockState p_222782_, SculkCatalystBlockEntity blockEntity, CallbackInfo ci) {
        CraftEventFactory.sourceBlockOverride = null;
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void arclight$load(CompoundTag p_277597_, CallbackInfo ci) {
        super.load(p_277597_);
    }
}
