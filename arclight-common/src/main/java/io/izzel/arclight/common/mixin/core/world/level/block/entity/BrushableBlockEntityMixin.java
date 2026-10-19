package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.util.ListUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.block.BlockBrushEvent;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin extends BlockEntityMixin {

    @WrapOperation(method = "brush", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$brushState(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
                                        long gameTime, ServerLevel world, LivingEntity user, Direction direction, ItemStack brush) {
        if (!(user instanceof ServerPlayer player)) {
            return original.call(level, pos, state, flags);
        }
        var event = CraftEventFactory.callBlockBrushEvent(level, pos, state, flags, player);
        if (!event.isCancelled()) {
            return event.getNewState().update(true);
        }
        return false;
    }

    @Inject(method = "brushingCompleted", at = @At("HEAD"), cancellable = true)
    private void arclight$completeBrush(ServerLevel level, LivingEntity user, ItemStack brush, CallbackInfo ci,
                                        @Share("brushEvent") LocalRef<BlockBrushEvent> eventRef,
                                        @Share("brushOldState") LocalRef<BlockState> oldStateRef) {
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        var oldState = this.getBlockState();
        var block = this.getBlockState().getBlock();
        var replacement = block instanceof BrushableBlock brushable ? brushable.getTurnsInto() : Blocks.AIR;
        oldStateRef.set(oldState);
        var event = CraftEventFactory.callBlockBrushEvent(level, this.worldPosition, replacement.defaultBlockState(), 3, player);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        eventRef.set(event);
    }

    @ModifyArg(method = "brushingCompleted", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), index = 2)
    private int arclight$brushOldState(int data, @Share("brushOldState") LocalRef<BlockState> oldStateRef) {
        return oldStateRef.get() == null ? data : Block.getId(oldStateRef.get());
    }

    @WrapOperation(method = "brushingCompleted", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$completeState(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
                                          @Share("brushEvent") LocalRef<BlockBrushEvent> eventRef) {
        var event = eventRef.get();
        return event == null ? original.call(level, pos, state, flags) : event.getNewState().update(true);
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
