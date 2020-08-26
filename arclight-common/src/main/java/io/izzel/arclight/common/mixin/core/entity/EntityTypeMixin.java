package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.entity.EntityTypeBridge;
import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
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
    @Shadow @Nullable public abstract T create(ServerWorld worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean p_220349_7_, boolean p_220349_8_);
    // @formatter:on

    @Inject(method = "spawn(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$spawnReason(ServerWorld worldIn, CompoundNBT compound, ITextComponent customName, PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean p_220342_7_, boolean p_220342_8_, CallbackInfoReturnable<T> cir) {
        CreatureSpawnEvent.SpawnReason spawnReason = ((IWorldWriterBridge) worldIn).bridge$getAddEntityReason();
        if (spawnReason == null) {
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        }
    }

    @Inject(method = "spawn(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V")))
    private void arclight$returnIfSuccess(ServerWorld worldIn, CompoundNBT compound, ITextComponent customName, PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean p_220342_7_, boolean p_220342_8_, CallbackInfoReturnable<T> cir, T t) {
        if (t != null) {
            cir.setReturnValue(t.removed ? null : t);
        }
    }

    public T spawnCreature(ServerWorld worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean p_220342_7_, boolean p_220342_8_, CreatureSpawnEvent.SpawnReason spawnReason) {
        T t = this.create(worldIn, compound, customName, playerIn, pos, reason, p_220342_7_, p_220342_8_);
        if (t != null) {
            if (t instanceof net.minecraft.entity.MobEntity && net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn((net.minecraft.entity.MobEntity) t, worldIn, pos.getX(), pos.getY(), pos.getZ(), null, reason))
                return null;
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(spawnReason);
            worldIn.func_242417_l(t);
            return t.removed ? null : t;
        }
        return null;
    }

    @Override
    public T bridge$spawnCreature(ServerWorld worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean flag, boolean flag1, CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawnCreature(worldIn, compound, customName, playerIn, pos, reason, flag, flag1, spawnReason);
    }
}
