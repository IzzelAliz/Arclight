package io.izzel.arclight.neoforge.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.neoforge.mixin.core.world.entity.LivingEntityMixin_NeoForge;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin_NeoForge extends LivingEntityMixin_NeoForge implements PlayerEntityBridge, IPlayerExtension {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    @Shadow public AbstractContainerMenu containerMenu;
    // @formatter:on

    @Shadow public abstract boolean isCreative();

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void arclight$onPlayerAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!CommonHooks.onEntityIncomingDamage((Player) (Object) this, new DamageContainer(source, amount))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack", cancellable = true, at = @At("HEAD"))
    private void arclight$onPlayerAttackTarget(Entity entity, CallbackInfo ci) {
        if (!CommonHooks.onPlayerAttackTarget((Player) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Override
    public boolean bridge$platform$mayfly() {
        return this.mayFly();
    }
}
