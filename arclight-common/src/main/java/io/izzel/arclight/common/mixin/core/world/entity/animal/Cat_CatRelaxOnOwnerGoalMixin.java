package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiConsumer;

@Mixin(targets = "net.minecraft.world.entity.animal.feline.Cat$CatRelaxOnOwnerGoal")
public class Cat_CatRelaxOnOwnerGoalMixin {

    @Shadow @Final private Cat cat;

    @Redirect(method = "giveMorningGift", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/feline/Cat;dropFromGiftLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/BiConsumer;)Z"))
    private boolean arclight$dropItem(Cat instance, ServerLevel level, ResourceKey<LootTable> lootTable, BiConsumer<ServerLevel, ItemStack> dropper) {
        return instance.dropFromGiftLootTable(level, lootTable, (serverLevel, itemStack) -> {
            var pos = this.cat.blockPosition();
            var itemEntity = new ItemEntity(serverLevel,
                (double) pos.getX() - (double) Mth.sin(this.cat.yBodyRot * ((float) Math.PI / 180F)),
                (double) pos.getY(),
                (double) pos.getZ() + (double) Mth.cos(this.cat.yBodyRot * ((float) Math.PI / 180F)),
                itemStack);
            var event = new EntityDropItemEvent(this.cat.bridge$getBukkitEntity(), (Item) itemEntity.bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                serverLevel.addFreshEntity(itemEntity);
            }
        });
    }
}
