package io.izzel.arclight.common.mixin.optimization.dfu;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityType.Builder.class)
public class EntityType_BuilderMixin_DFU {

    @Redirect(method = "build", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EntityType$Builder;serialize:Z"))
    private boolean arclight$noDfu(EntityType.Builder<?> builder) {
        return false;
    }
}
