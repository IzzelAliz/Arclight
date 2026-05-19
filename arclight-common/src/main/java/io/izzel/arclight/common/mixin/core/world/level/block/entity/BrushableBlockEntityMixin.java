package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.util.ListUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin extends BlockEntityMixin {

    @Redirect(method = "dropContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$drop(ServerLevel instance, Entity entity, ServerLevel level, LivingEntity livingEntity, ItemStack itemStack) {
        if (!(livingEntity instanceof ServerPlayer player)) {
            return instance.addFreshEntity(entity);
        }
        var block = CraftBlock.at(this.level, this.worldPosition);
        CraftEventFactory.handleBlockDropItemEvent(block, block.getState(), player, ListUtil.asMutableList((ItemEntity) entity));
        return true;
    }
}
