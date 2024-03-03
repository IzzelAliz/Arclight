package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow private BlockState blockState;
    @Shadow public static FallingBlockEntity fall(Level p_201972_, BlockPos p_201973_, BlockState p_201974_) { return null; }
    // @formatter:on

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void arclight$despawn1(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void arclight$despawn2(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 5, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void arclight$despawn3(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void arclight$drop1(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void arclight$drop2(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"))
    private void arclight$drop3(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$entityChangeBlock(CallbackInfo ci, Block block, BlockPos pos) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((FallingBlockEntity) (Object) this, pos, this.blockState)) {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
            this.discard();
            ci.cancel();
        }
    }

    @Inject(method = "fall", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void arclight$entityFall(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir, FallingBlockEntity entity) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state.getFluidState().createLegacyBlock())) {
            cir.setReturnValue(entity);
        }
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static FallingBlockEntity fall(Level level, BlockPos pos, BlockState state, CreatureSpawnEvent.SpawnReason spawnReason) {
        ((WorldBridge) level).bridge$pushAddEntityReason(spawnReason);
        return fall(level, pos, state);
    }
}
