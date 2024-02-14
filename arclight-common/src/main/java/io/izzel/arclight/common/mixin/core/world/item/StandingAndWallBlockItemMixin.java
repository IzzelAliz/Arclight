package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StandingAndWallBlockItem.class)
public class StandingAndWallBlockItemMixin {

    @Inject(method = "getPlacementState", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelReader;isUnobstructed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Z"))
    private void arclight$blockCanPlace(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir, BlockState place, BlockState defaultReturn) {
        if (defaultReturn != null) {
            var result = context.getLevel().isUnobstructed(defaultReturn, context.getClickedPos(), CollisionContext.empty());
            var player = (context.getPlayer() instanceof ServerPlayerEntityBridge bridge) ? bridge.bridge$getBukkitEntity() : null;

            var event = new BlockCanBuildEvent(CraftBlock.at(context.getLevel(), context.getClickedPos()), player, CraftBlockData.fromData(defaultReturn), result);
            Bukkit.getPluginManager().callEvent(event);

            cir.setReturnValue(event.isBuildable() ? defaultReturn : null);
        }
    }
}
