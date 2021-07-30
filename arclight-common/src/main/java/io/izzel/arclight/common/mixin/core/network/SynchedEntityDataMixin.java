package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.EntityDataManagerBridge;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin implements EntityDataManagerBridge {

    // @formatter:off
    @Shadow protected abstract <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> key);
    @Shadow private boolean isDirty;
    @Shadow @Final private Entity entity;
    // @formatter:on

    @Inject(method = "set", at = @At("HEAD"))
    private <T> void arclight$syncHealth(EntityDataAccessor<T> key, T value, CallbackInfo ci) {
        if (key == LivingEntity.DATA_HEALTH_ID && this.entity instanceof ServerPlayerEntityBridge
            && ((ServerPlayerEntityBridge) this.entity).bridge$initialized()) {
            CraftPlayer player = ((ServerPlayerEntityBridge) this.entity).bridge$getBukkitEntity();
            player.setRealHealth(((Float) value));
        }
    }

    public <T> void markDirty(EntityDataAccessor<T> key) {
        SynchedEntityData.DataItem<T> entry = this.getItem(key);
        entry.setDirty(true);
        this.isDirty = true;
    }

    @Override
    public <T> void bridge$markDirty(EntityDataAccessor<T> key) {
        this.markDirty(key);
    }
}
