package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
    @ModifyReturnValue(method = "onlyOpCanSetNbt", at = @At("RETURN"))
    private boolean arclight$chestNbtPermission(boolean original) {
        return original || (Object) this == BlockEntityType.CHEST;
    }
}
