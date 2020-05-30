package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HangingEntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HangingEntityItem.class)
public class HangingEntityItemMixin {

    @Inject(method = "onItemUse", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/HangingEntity;playPlaceSound()V"))
    public void arclight$hangingPlace(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir, BlockPos blockPos, Direction direction, BlockPos blockPos1, PlayerEntity playerEntity, ItemStack itemStack, World world, HangingEntity hangingEntity) {
        Player who = (context.getPlayer() == null) ? null : (Player) ((PlayerEntityBridge) context.getPlayer()).bridge$getBukkitEntity();
        Block blockClicked = CraftBlock.at(world, blockPos);
        BlockFace blockFace = CraftBlock.notchToBlockFace(direction);

        HangingPlaceEvent event = new HangingPlaceEvent((Hanging) ((EntityBridge) hangingEntity).bridge$getBukkitEntity(), who, blockClicked, blockFace);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(ActionResultType.FAIL);
        }
    }
}
