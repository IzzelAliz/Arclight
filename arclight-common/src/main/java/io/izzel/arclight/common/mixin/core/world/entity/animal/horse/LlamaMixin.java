package io.izzel.arclight.common.mixin.core.world.entity.animal.horse;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.world.entity.animal.equine.Llama;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Llama.class)
public abstract class LlamaMixin extends AbstractHorseMixin {

    @WrapOperation(method = "handleEating(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/Llama;heal(F)V"))
    private void arclight$localHealReason(Llama receiver, float amount, Operation<Void> original) {
        LivingEntityBridge bridge = (LivingEntityBridge) receiver;
        EntityRegainHealthEvent.RegainReason previous = bridge.bridge$peekHealReason();
        bridge.bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING);
        try {
            original.call(receiver, amount);
        } finally {
            bridge.bridge$pushHealReason(previous);
        }
    }

    // @formatter:off
    @Shadow private void setStrength(int p_30841_) {}
    // @formatter:on

    public void setStrengthPublic(int i) {
        this.setStrength(i);
    }
}
