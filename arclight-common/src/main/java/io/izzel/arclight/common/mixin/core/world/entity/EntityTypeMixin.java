package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityTypeBridge;
import io.izzel.arclight.common.bridge.core.world.IWorldWriterBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> implements EntityTypeBridge<T> {

    // @formatter:off
    @Shadow @Nullable public abstract T create(ServerLevel worldIn, @Nullable CompoundTag compound, @Nullable Component customName, @Nullable Player playerIn, BlockPos pos, MobSpawnType reason, boolean p_220349_7_, boolean p_220349_8_);
    // @formatter:on

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$spawnReason(ServerLevel worldIn, CompoundTag compound, Component customName, Player playerIn, BlockPos pos, MobSpawnType reason, boolean p_220342_7_, boolean p_220342_8_, CallbackInfoReturnable<T> cir) {
        CreatureSpawnEvent.SpawnReason spawnReason = ((IWorldWriterBridge) worldIn).bridge$getAddEntityReason();
        if (spawnReason == null) {
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        }
    }

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V")))
    private void arclight$returnIfSuccess(ServerLevel worldIn, CompoundTag compound, Component customName, Player playerIn, BlockPos pos, MobSpawnType reason, boolean p_220342_7_, boolean p_220342_8_, CallbackInfoReturnable<T> cir, T t) {
        if (t != null) {
            cir.setReturnValue(t.isRemoved() ? null : t);
        }
    }

    public T spawn(ServerLevel worldIn, @Nullable CompoundTag compound, @Nullable Component customName, @Nullable Player playerIn, BlockPos pos, MobSpawnType reason, boolean p_220342_7_, boolean p_220342_8_, CreatureSpawnEvent.SpawnReason spawnReason) {
        T t = this.create(worldIn, compound, customName, playerIn, pos, reason, p_220342_7_, p_220342_8_);
        if (t != null) {
            if (t instanceof net.minecraft.world.entity.Mob && net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn((net.minecraft.world.entity.Mob) t, worldIn, pos.getX(), pos.getY(), pos.getZ(), null, reason))
                return null;
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(spawnReason);
            worldIn.addFreshEntityWithPassengers(t);
            return t.isRemoved() ? null : t;
        }
        return null;
    }

    @Override
    public T bridge$spawnCreature(ServerLevel worldIn, @Nullable CompoundTag compound, @Nullable Component customName, @Nullable Player playerIn, BlockPos pos, MobSpawnType reason, boolean flag, boolean flag1, CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawn(worldIn, compound, customName, playerIn, pos, reason, flag, flag1, spawnReason);
    }
}
