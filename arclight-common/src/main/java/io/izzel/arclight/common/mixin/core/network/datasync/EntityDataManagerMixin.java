package io.izzel.arclight.common.mixin.core.network.datasync;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.datasync.EntityDataManagerBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityDataManager.class)
public abstract class EntityDataManagerMixin implements EntityDataManagerBridge {

    // @formatter:off
    @Shadow protected abstract <T> EntityDataManager.DataEntry<T> getEntry(DataParameter<T> key);
    @Shadow private boolean dirty;
    @Shadow @Final private Entity entity;
    // @formatter:on

    @Inject(method = "set", at = @At("HEAD"))
    private <T> void arclight$syncHealth(DataParameter<T> key, T value, CallbackInfo ci) {
        if (key == LivingEntity.HEALTH && this.entity instanceof ServerPlayerEntityBridge
            && ((ServerPlayerEntityBridge) this.entity).bridge$initialized()) {
            CraftPlayer player = ((ServerPlayerEntityBridge) this.entity).bridge$getBukkitEntity();
            player.setRealHealth(((Float) value));
        }
    }

    public <T> void markDirty(DataParameter<T> key) {
        EntityDataManager.DataEntry<T> entry = this.getEntry(key);
        entry.setDirty(true);
        this.dirty = true;
    }

    @Override
    public <T> void bridge$markDirty(DataParameter<T> key) {
        this.markDirty(key);
    }
}
