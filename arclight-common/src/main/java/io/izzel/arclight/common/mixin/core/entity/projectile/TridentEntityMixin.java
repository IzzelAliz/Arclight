package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.projectile.TridentEntityBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.EntityRayTraceResult;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends AbstractArrowEntityMixin implements TridentEntityBridge {

    @Shadow public ItemStack thrownStack;

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addLightningBolt(Lnet/minecraft/entity/effect/LightningBoltEntity;)V"))
    private void arclight$lightning(EntityRayTraceResult p_213868_1_, CallbackInfo ci) {
        ((ServerWorldBridge) this.world).bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause.TRIDENT);
    }

    @Override
    public void bridge$setThrownStack(ItemStack itemStack) {
        this.thrownStack = itemStack;
    }
}
