package io.izzel.arclight.common.mixin.core.world.raid;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.raid.Raid;
import net.minecraft.world.raid.RaidManager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(RaidManager.class)
public class RaidManagerMixin {

    // @formatter:off
    @Shadow @Final public Map<Integer, Raid> byId;
    // @formatter:on

    @Inject(method = "badOmenTick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;increaseLevel(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    public void arclight$raidTrigger(ServerPlayerEntity playerEntity, CallbackInfoReturnable<Raid> cir,
                                     DimensionType dimensionType, BlockPos pos, BlockPos pos1, Raid raid) {
        if (!CraftEventFactory.callRaidTriggerEvent(raid, playerEntity)) {
            playerEntity.removePotionEffect(Effects.BAD_OMEN);
            this.byId.remove(raid.getId(), raid);
            cir.setReturnValue(null);
        }
    }
}
