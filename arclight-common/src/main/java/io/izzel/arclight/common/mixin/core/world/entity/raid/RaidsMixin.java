package io.izzel.arclight.common.mixin.core.world.entity.raid;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(Raids.class)
public class RaidsMixin {

    // @formatter:off
    @Shadow @Final public Int2ObjectMap<Raid> raidMap;
    // @formatter:on

    @Inject(method = "createOrExtendRaid", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;absorbRaidOmen(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    public void arclight$raidTrigger(ServerPlayer playerEntity, BlockPos pos, CallbackInfoReturnable<Raid> cir,
                                     ServerLevel serverLevel, List<?> list, int i, Vec3 vec, BlockPos pos1, Raid raid) {
        if (!CraftEventFactory.callRaidTriggerEvent(raid, playerEntity.level(), playerEntity)) {
            playerEntity.removeEffect(MobEffects.RAID_OMEN);
            this.raidMap.values().removeIf(value -> value == raid);
            cir.setReturnValue(null);
        }
    }
}
