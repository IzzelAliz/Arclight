package io.izzel.arclight.common.mixin.core.world.entity.raid;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raids.class)
public class RaidsMixin {
    // Fire before the native registration so cancellation never erases an existing raid.
    @ModifyExpressionValue(method = "createOrExtendRaid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;getOrCreateRaid(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/entity/raid/Raid;"))
    private Raid arclight$raidTrigger(Raid raid, ServerPlayer player, BlockPos pos,
            @Cancellable CallbackInfoReturnable<Raid> cir) {
        if ((!raid.isStarted() || ((!raid.isStopped() && !raid.isOver()) && raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()))
                && !CraftEventFactory.callRaidTriggerEvent(player.level(), raid, player)) {
            player.removeEffect(MobEffects.RAID_OMEN);
            cir.setReturnValue(null);
        }
        return raid;
    }

    @WrapOperation(method = "createOrExtendRaid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;absorbRaidOmen(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean arclight$onlyActiveRaidAbsorbs(Raid raid, ServerPlayer player, Operation<Boolean> original) {
        return (!raid.isStarted() || (!raid.isStopped() && !raid.isOver())) && original.call(raid, player);
    }
}
