package io.izzel.arclight.forge.mixin.core.world.entity.ai.behavior;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HarvestFarmland.class)
public abstract class HarvestFarmlandMixin_Forge {

    // This call belongs only to Forge's IPlantable/CROP branch. Common handles
    // vanilla setBlockAndUpdate; do not duplicate that event or bypass Forge's gate.
    @WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
        require = 1, expect = 1, allow = 1)
    private boolean arclight$plant(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
                                   ServerLevel hostLevel, Villager owner, long time,
                                   @Share("forgePlantVeto") LocalBooleanRef veto) {
        boolean allowed = CraftEventFactory.callEntityChangeBlockEvent(owner, pos, state);
        veto.set(!allowed);
        return allowed && original.call(level, pos, state, flags);
    }

    @ModifyVariable(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
        at = @At("STORE"), ordinal = 0)
    private boolean arclight$plantOk(boolean ok, @Share("forgePlantVeto") LocalBooleanRef veto) {
        if (!ok) veto.set(false); // Native per-inventory-slot initialization.
        return ok && !veto.get(); // Veto must also suppress sound and seed consumption.
    }
}
