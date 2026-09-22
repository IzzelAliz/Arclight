package io.izzel.arclight.common.mixin.core.world.entity.animal.sniffer;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Sniffer.class)
public abstract class SnifferMixin extends AnimalMixin {

    @Shadow private BlockPos getHeadBlock() { return null; }

    @ModifyArg(method = "dropSeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sniffer/Sniffer;dropFromGiftLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/BiConsumer;)Z"), index = 2)
    private java.util.function.BiConsumer<ServerLevel, ItemStack> arclight$dropSeed(java.util.function.BiConsumer<ServerLevel, ItemStack> consumer) {
        return (instance, stack) -> {
            BlockPos pos = this.getHeadBlock();
            ItemEntity entity = new ItemEntity(this.level(), pos.getX(), pos.getY(), pos.getZ(), stack);
            entity.setDefaultPickUpDelay();
            var event = new EntityDropItemEvent(this.getBukkitEntity(), (Item) ((EntityBridge) entity).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                instance.addFreshEntity(entity);
            }
        };
    }
}
