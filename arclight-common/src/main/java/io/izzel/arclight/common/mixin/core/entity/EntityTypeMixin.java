package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.entity.EntityTypeBridge;
import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> implements EntityTypeBridge<T> {

    // @formatter:off
    @Shadow @Nullable public abstract T create(World worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean p_220349_7_, boolean p_220349_8_);
    // @formatter:on

    @Inject(method = "spawn(Lnet/minecraft/world/World;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;", at = @At("HEAD"))
    public void arclight$spawnReason(World worldIn, CompoundNBT compound, ITextComponent customName, PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean p_220342_7_, boolean p_220342_8_, CallbackInfoReturnable<T> cir) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
    }

    public T spawnCreature(World worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean flag, boolean flag1, CreatureSpawnEvent.SpawnReason spawnReason) {
        T t = this.create(worldIn, compound, customName, playerIn, pos, reason, flag, flag1);
        if (t instanceof MobEntity && ForgeEventFactory.doSpecialSpawn((MobEntity) t, worldIn, pos.getX(), pos.getY(), pos.getZ(), null, reason)) {
            return null;
        }
        return ((IWorldWriterBridge) worldIn).bridge$addEntity(t, spawnReason) ? t : null;
    }

    @Override
    public T bridge$spawnCreature(World worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean flag, boolean flag1, CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawnCreature(worldIn, compound, customName, playerIn, pos, reason, flag, flag1, spawnReason);
    }
}
