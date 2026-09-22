package io.izzel.arclight.common.mixin.vanilla.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

@Mixin(Sheep.class)
public abstract class SheepMixin_Vanilla extends AnimalMixin {

    @Decorate(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)V"))
    private void arclight$forceDrop(Sheep sheep, ServerLevel level, ResourceKey<LootTable> lootTable, ItemStack stack, BiConsumer<ServerLevel, ItemStack> dropper) throws Throwable {
        boolean previous = this.bridge$isForceDrops();
        this.bridge$setForceDrops(true);
        try {
            DecorationOps.callsite().invoke(sheep, level, lootTable, stack, dropper);
        } finally {
            this.bridge$setForceDrops(previous);
        }
    }
}
