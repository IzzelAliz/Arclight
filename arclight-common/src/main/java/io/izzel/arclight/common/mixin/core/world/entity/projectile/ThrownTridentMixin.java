package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.world.entity.projectile.ThrownTridentBridge;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrowMixin implements ThrownTridentBridge {

    @Shadow @Final private static EntityDataAccessor<Byte> ID_LOYALTY;
    @Shadow @Final private static EntityDataAccessor<Boolean> ID_FOIL;

    @Override
    public void setFoil(boolean foil) {
        this.entityData.set(ID_FOIL, foil);
    }

    @Override
    public int getLoyalty() {
        return this.entityData.get(ID_LOYALTY);
    }

    @Override
    public void setLoyalty(byte loyalty) {
        this.entityData.set(ID_LOYALTY, loyalty);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;discard()V"))
    private void arclight$dropCause(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;discard()V"))
    private void arclight$despawnCause(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Override
    public void bridge$setThrownStack(ItemStack itemStack) {
        this.pickupItemStack = itemStack;
    }
}
