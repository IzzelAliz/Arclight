package io.izzel.arclight.common.mixin.core.world.entity.animal.golem;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin {

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$statueChangeBlock(ServerLevel level, BlockPos pos, BlockState state, int flags) throws Throwable {
        if (!CraftEventFactory.callEntityChangeBlockEvent((CopperGolem) (Object) this, pos, state)) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, state, flags);
    }

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;dropPreservedEquipment(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$statueForceDrops(CopperGolem golem, ServerLevel level) throws Throwable {
        var bridge = (EntityBridge) golem;
        boolean previous = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            DecorationOps.callsite().invoke(golem, level);
        } finally {
            bridge.bridge$setForceDrops(previous);
        }
    }

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;discard()V"))
    private void arclight$statueRemoveCause(CopperGolem golem) throws Throwable {
        ((EntityBridge) golem).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
        DecorationOps.callsite().invoke(golem);
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    private void arclight$directShearGate(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Local(ordinal = 0) ItemStack stack) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (CopperGolem) (Object) this, stack, hand)) {
            ((PlayerBridge) player).bridge$cancelCopperShear((CopperGolem) (Object) this);
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
