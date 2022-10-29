package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
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

    @Inject(method = "getPlacementState", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private void arclight$blockCanPlace(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir, BlockState place, BlockState defaultReturn) {
        if (defaultReturn != null) {
            var result = cir.getReturnValue() != null;
            var player = (context.getPlayer() instanceof ServerPlayerEntityBridge bridge) ? bridge.bridge$getBukkitEntity() : null;

            var event = new BlockCanBuildEvent(CraftBlock.at(context.getLevel(), context.getClickedPos()), player, CraftBlockData.fromData(defaultReturn), result);
            Bukkit.getPluginManager().callEvent(event);

            cir.setReturnValue(event.isBuildable() ? defaultReturn : null);
        }
    }
}
