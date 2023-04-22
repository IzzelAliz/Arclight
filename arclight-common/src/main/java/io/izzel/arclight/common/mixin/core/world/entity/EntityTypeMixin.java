package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityTypeBridge;
import io.izzel.arclight.common.bridge.core.world.IWorldWriterBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> implements EntityTypeBridge<T> {

    // @formatter:off
    @Shadow @Nullable public abstract T create(ServerLevel p_262637_, @org.jetbrains.annotations.Nullable CompoundTag p_262687_, @org.jetbrains.annotations.Nullable Consumer<T> p_262629_, BlockPos p_262595_, MobSpawnType p_262666_, boolean p_262685_, boolean p_262588_);
    // @formatter:on

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
        at = @At(value = "HEAD"))
    private void arclight$spawnReason(ServerLevel worldIn, ItemStack p_20594_, Player p_20595_, BlockPos p_20596_, MobSpawnType p_20597_, boolean p_20598_, boolean p_20599_, CallbackInfoReturnable<T> cir) {
        CreatureSpawnEvent.SpawnReason spawnReason = ((IWorldWriterBridge) worldIn).bridge$getAddEntityReason();
        if (spawnReason == null) {
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        }
    }

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/nbt/CompoundTag;Ljava/util/function/Consumer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V")))
    private void arclight$returnIfSuccess(ServerLevel p_262704_, CompoundTag p_262603_, Consumer<T> p_262621_, BlockPos p_262672_, MobSpawnType p_262644_, boolean p_262690_, boolean p_262590_, CallbackInfoReturnable<T> cir, T t) {
        if (t != null) {
            cir.setReturnValue(t.isRemoved() ? null : t);
        }
    }

    public T spawn(ServerLevel p_262634_, BlockPos p_262707_, MobSpawnType p_262597_, CreatureSpawnEvent.SpawnReason spawnReason) {
        return this.spawn(p_262634_, null, null, p_262707_, p_262597_, false, false, spawnReason);
    }

    public T spawn(ServerLevel p_262704_, @Nullable CompoundTag p_262603_, @Nullable Consumer<T> p_262621_, BlockPos p_262672_, MobSpawnType p_262644_, boolean p_262690_, boolean p_262590_, CreatureSpawnEvent.SpawnReason spawnReason) {
        T t = this.create(p_262704_, p_262603_, p_262621_, p_262672_, p_262644_, p_262690_, p_262590_);
        if (t != null) {
            ((IWorldWriterBridge) p_262704_).bridge$pushAddEntityReason(spawnReason);
            p_262704_.addFreshEntityWithPassengers(t);
            return t.isRemoved() ? null : t;
        }
        return null;
    }

    @Override
    public T bridge$spawnCreature(ServerLevel worldIn, BlockPos pos, MobSpawnType mobSpawnType, CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawn(worldIn, pos, mobSpawnType, spawnReason);
    }
}
