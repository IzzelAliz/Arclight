package io.izzel.arclight.common.mixin.optimization.dfu;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin_DFU {

    @Redirect(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;fetchChoiceType(Lcom/mojang/datafixers/DSL$TypeReference;Ljava/lang/String;)Lcom/mojang/datafixers/types/Type;"))
    private static Type<?> arclight$noDfu(DSL.TypeReference p_137457_, String p_137458_) {
        return null;
    }
}
