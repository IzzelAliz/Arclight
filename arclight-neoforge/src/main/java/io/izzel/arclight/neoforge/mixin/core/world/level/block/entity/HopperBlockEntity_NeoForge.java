package io.izzel.arclight.neoforge.mixin.core.world.level.block.entity;

import io.izzel.arclight.neoforge.mod.util.DelegatedContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntity_NeoForge {

    @Inject(method = "ejectItems", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getAttachedContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Lnet/minecraft/world/Container;"))
    private static void arclight$cancelIfFound(Level level, BlockPos blockPos, HopperBlockEntity hopperBlockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (DelegatedContainer.foundLastHandler()) {
            cir.setReturnValue(false);
        }
    }
}
