package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.villager.Villager;

@Mixin(HarvestFarmland.class)
public abstract class HarvestFarmlandMixin {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean plant(ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original, ServerLevel hostLevel, Villager owner, long time, @com.llamalad7.mixinextras.sugar.Share("plantVeto") com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef veto) {
        boolean allowed = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityChangeBlockEvent(owner, pos, state);
        veto.set(!allowed);
        return allowed && original.call(level, pos, state);
    }

    @com.llamalad7.mixinextras.injector.WrapWithCondition(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"))
    private boolean plantGameEvent(ServerLevel level, net.minecraft.core.Holder<?> event, net.minecraft.core.BlockPos pos, net.minecraft.world.level.gameevent.GameEvent.Context context, @com.llamalad7.mixinextras.sugar.Share("plantVeto") com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef veto) {
        return !veto.get();
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V", at = @At("STORE"), ordinal = 0)
    private boolean plantOk(boolean ok, @com.llamalad7.mixinextras.sugar.Share("plantVeto") com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef veto) {
        if (!ok) veto.set(false); // native per-slot initialization; SpecialPlantable remains independent
        return ok && !veto.get();
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"), require = 1, expect = 1, allow = 1)
    private boolean onDestroy(ServerLevel level, net.minecraft.core.BlockPos pos, boolean dropResources, net.minecraft.world.entity.Entity breaker, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original, ServerLevel hostLevel, Villager owner, long gameTime) {
        if (!org.bukkit.craftbukkit.event.CraftEventFactory.callEntityChangeBlockEvent(owner, pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())) {
            return false;
        }
        return original.call(level, pos, dropResources, breaker);
    }
}
