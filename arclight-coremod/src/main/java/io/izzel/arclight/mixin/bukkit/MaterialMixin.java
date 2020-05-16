package io.izzel.arclight.mixin.bukkit;

import io.izzel.arclight.bridge.bukkit.MaterialBridge;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Material.class)
public abstract class MaterialMixin implements MaterialBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract boolean isBlock();

    @Override @Accessor(value = "key", remap = false)
    public abstract void bridge$setKey(NamespacedKey namespacedKey);
    // @formatter:on

    private net.minecraft.block.material.Material arclight$internal;
    private Boolean arclight$isBlock;
    private Boolean arclight$isItem;

    @Override
    public void bridge$setInternal(net.minecraft.block.material.Material internal) {
        this.arclight$internal = internal;
    }

    @Inject(method = "isBlock", cancellable = true, remap = false, at = @At("HEAD"))
    public void arclight$isBlock(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$isBlock != null) {
            cir.setReturnValue(arclight$isBlock);
            return;
        }
        if (arclight$internal != null) {
            cir.setReturnValue(arclight$internal.isReplaceable());
        }
    }

    @Inject(method = "isItem", cancellable = true, remap = false, at = @At("HEAD"))
    private void arclight$isItem(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$isItem != null) {
            cir.setReturnValue(arclight$isItem);
        }
    }

    @Inject(method = "isSolid", cancellable = true, remap = false, at = @At("HEAD"))
    public void arclight$isSolid(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$internal != null) {
            cir.setReturnValue(arclight$internal.isSolid());
        }
    }

    @Inject(method = "isTransparent", cancellable = true, remap = false, at = @At("HEAD"))
    public void arclight$isTransparent(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$internal != null) {
            cir.setReturnValue(this.isBlock() && !arclight$internal.isOpaque());
        }
    }

    @Inject(method = "isFlammable", cancellable = true, remap = false, at = @At("HEAD"))
    public void arclight$isLiquid(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$internal != null) {
            cir.setReturnValue(arclight$internal.isFlammable());
        }
    }

    @Inject(method = "isOccluding", cancellable = true, remap = false, at = @At("HEAD"))
    public void arclight$isOccluding(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$internal != null) {
            cir.setReturnValue(arclight$internal.isOpaque());
        }
    }

    @Override
    public void bridge$setItem() {
        arclight$isItem = Boolean.TRUE;
    }

    @Override
    public void bridge$setBlock() {
        arclight$isBlock = Boolean.TRUE;
    }
}
