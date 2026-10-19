package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.DoubleChestMenuProviderBridge;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("public-target")
@Mixin(targets = "net/minecraft/world/level/block/ChestBlock$2$1")
public class ChestBlock2_1Mixin implements DoubleChestMenuProviderBridge {

    @Shadow @Final private Container val$container;

    public CompoundContainer container;
    public CompoundContainer inventorylargechest;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$bindContainer(CallbackInfo ci) {
        this.container = (CompoundContainer) this.val$container;
        this.inventorylargechest = this.container;
    }

    @Override
    public CompoundContainer bridge$getDoubleChestContainer() {
        return (CompoundContainer) this.val$container;
    }
}
