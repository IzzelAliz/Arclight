package io.izzel.arclight.common.mixin.core.world.raid;

import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.dimension.DimensionType;

@Mixin(Raids.class)
public class RaidManagerMixin {

    // @formatter:off
    @Shadow @Final public Map<Integer, Raid> raidMap;
    // @formatter:on

    @Inject(method = "createOrExtendRaid", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;absorbBadOmen(Lnet/minecraft/world/entity/player/Player;)V"))
    public void arclight$raidTrigger(ServerPlayer playerEntity, CallbackInfoReturnable<Raid> cir,
                                     DimensionType dimensionType, BlockPos pos, BlockPos pos1, Raid raid) {
        if (!CraftEventFactory.callRaidTriggerEvent(raid, playerEntity)) {
            playerEntity.removeEffect(MobEffects.BAD_OMEN);
            this.raidMap.remove(raid.getId(), raid);
            cir.setReturnValue(null);
        }
    }
}
