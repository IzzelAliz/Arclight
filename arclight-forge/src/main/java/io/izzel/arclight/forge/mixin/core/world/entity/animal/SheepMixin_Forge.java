package io.izzel.arclight.forge.mixin.core.world.entity.animal;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sheep.class)
public abstract class SheepMixin_Forge extends AnimalMixin {

    // Forge collects shear loot first; direct shearing emits it from this helper.
    @Decorate(method = "dropItems(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;)V", remap = false,
        at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"),
        require = 1, expect = 1, allow = 1)
    private ItemEntity arclight$forceDrop(Sheep sheep, ServerLevel level, ItemStack stack, float offset) throws Throwable {
        boolean previous = this.bridge$isForceDrops();
        this.bridge$setForceDrops(true);
        try {
            return (ItemEntity) DecorationOps.callsite().invoke(sheep, level, stack, offset);
        } finally {
            this.bridge$setForceDrops(previous);
        }
    }
}