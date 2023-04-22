package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin implements SynchedEntityDataBridge {

    // @formatter:off
    @Shadow protected abstract <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> key);
    @Shadow private boolean isDirty;
    @Shadow @Final private Entity entity;
    @Shadow @Nullable public abstract List<SynchedEntityData.DataValue<?>> getNonDefaultValues();
    @Shadow public abstract boolean isEmpty();
    // @formatter:on

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("HEAD"))
    private <T> void arclight$syncHealth(EntityDataAccessor<T> key, T value, boolean b, CallbackInfo ci) {
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

    public void refresh(ServerPlayer player) {
        if (!this.isEmpty()) {
            var list = this.getNonDefaultValues();
            if (list != null) {
                player.connection.send(new ClientboundSetEntityDataPacket(this.entity.getId(), list));
            }
        }
    }

    @Override
    public void bridge$refresh(ServerPlayer player) {
        refresh(player);
    }
}
