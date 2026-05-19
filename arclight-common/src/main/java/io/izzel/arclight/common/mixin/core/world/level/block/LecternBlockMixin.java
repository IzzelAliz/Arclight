package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LecternBlockEntity.class)
public class LecternBlockMixin {

    @Redirect(method = "preRemoveSideEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$returnIfEmpty(Level level, Entity entity) {
        if (entity instanceof ItemEntity itemEntity && itemEntity.getItem().isEmpty()) {
            return false;
        }
        return level.addFreshEntity(entity);
    }
}
