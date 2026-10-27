package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.util.ListUtil;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin extends BlockEntityMixin {

    @Shadow private int brushCount;
    @Shadow protected abstract int getCompletionState();

    @Inject(method = "brush", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BrushableBlockEntity;unpackLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemInstance;)V"), cancellable = true)
    private void arclight$brushChange(long gameTime, ServerLevel level, LivingEntity user, Direction direction, ItemStack brush, CallbackInfoReturnable<Boolean> cir) {
        int currentStage = this.getCompletionState();
        this.brushCount++;
        int nextStage = this.getCompletionState();
        boolean completed = this.brushCount >= 10;
        this.brushCount--;
        if (completed || currentStage != nextStage) {
            var state = this.getBlockState();
            var replacement = completed
                ? (state.getBlock() instanceof BrushableBlock brushable ? brushable.getTurnsInto() : Blocks.AIR).defaultBlockState()
                : state.setValue(BlockStateProperties.DUSTED, nextStage);
            if (!CraftEventFactory.callEntityChangeBlockEvent(user, this.worldPosition, replacement)) {
                cir.setReturnValue(false);
            }
        }
    }

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
