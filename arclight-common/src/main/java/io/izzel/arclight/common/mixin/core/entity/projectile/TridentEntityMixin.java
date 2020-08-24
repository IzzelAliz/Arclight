package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.projectile.TridentEntityBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends AbstractArrowEntityMixin implements TridentEntityBridge {

    @Shadow public ItemStack thrownStack;

    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$lightning(World world, Entity entityIn) {
        ((ServerWorldBridge) this.world).bridge$strikeLightning((LightningBoltEntity) entityIn, LightningStrikeEvent.Cause.TRIDENT);
        return true;
    }

    @Override
    public void bridge$setThrownStack(ItemStack itemStack) {
        this.thrownStack = itemStack;
    }
}
