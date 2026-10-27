package io.izzel.arclight.common.mixin.core.network.syncher;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin implements SynchedEntityDataBridge {

    // @formatter:off
    @Shadow protected abstract <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> key);
    @Shadow private boolean isDirty;
    @Shadow @Final private SyncedDataHolder entity;
    @Shadow @Final private SynchedEntityData.DataItem<?>[] itemsById;
    // @formatter:on

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("HEAD"))
    private <T> void arclight$syncHealth(EntityDataAccessor<T> key, T value, boolean b, CallbackInfo ci) {
        if (key == LivingEntity.DATA_HEALTH_ID && this.entity instanceof ServerPlayerBridge
            && ((ServerPlayerBridge) this.entity).bridge$initialized()) {
            CraftPlayer player = ((ServerPlayerBridge) this.entity).bridge$getBukkitEntity();
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
        if (!(this.entity instanceof Entity entity)) {
            return;
        }
        if (!((ServerPlayerBridge) player).bridge$getBukkitEntity()
            .canSee(((io.izzel.arclight.common.bridge.core.entity.EntityBridge) entity).bridge$getBukkitEntity())) {
            return;
        }
        List<SynchedEntityData.DataValue<?>> values = new java.util.ArrayList<>(this.itemsById.length);
        for (SynchedEntityData.DataItem<?> item : this.itemsById) {
            values.add(item.value());
        }
        player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), values));
    }

    @Override
    public void bridge$refresh(ServerPlayer player) {
        refresh(player);
    }
}
