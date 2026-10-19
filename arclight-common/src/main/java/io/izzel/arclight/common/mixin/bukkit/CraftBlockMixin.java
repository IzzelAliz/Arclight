package io.izzel.arclight.common.mixin.bukkit;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import net.minecraft.world.InteractionResult;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftBlock.class, remap = false)
public abstract class CraftBlockMixin {

    // @formatter:off
    @Shadow public abstract Material getType();
    // @formatter:on

    // Change only the comparison operand, not the native useOn result or event veto.
    @ModifyExpressionValue(method = "applyBoneMeal", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/InteractionResult;SUCCESS:Lnet/minecraft/world/InteractionResult$Success;"),
        require = 1, expect = 1, allow = 1)
    private InteractionResult.Success arclight$boneMealSuccess(InteractionResult.Success success,
            @Local InteractionResult result) {
        return result == InteractionResult.SUCCESS_SERVER ? InteractionResult.SUCCESS_SERVER : success;
    }

    @Inject(method = "getState", cancellable = true, at = @At("HEAD"))
    private void arclight$getState(CallbackInfoReturnable<BlockState> cir) {
        MaterialBridge bridge = (MaterialBridge) (Object) getType();
        if (bridge.bridge$shouldApplyStateFactory()) {
            cir.setReturnValue(bridge.bridge$blockStateFactory().apply((CraftBlock) (Object) this));
        }
    }
}
