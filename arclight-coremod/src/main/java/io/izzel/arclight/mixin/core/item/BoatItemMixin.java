package io.izzel.arclight.mixin.core.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoatItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BoatItem.class)
public class BoatItemMixin {

    @Inject(method = "onItemRightClick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "NEW", target = "net/minecraft/entity/item/BoatEntity"))
    public void arclight$playerInteract(World worldIn, PlayerEntity playerIn, Hand handIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir, ItemStack itemStack, RayTraceResult rayTraceResult) {
        BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) rayTraceResult;
        PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(playerIn, Action.RIGHT_CLICK_BLOCK, blockRayTraceResult.getPos(), blockRayTraceResult.getFace(), itemStack, handIn);

        if (event.isCancelled()) {
            cir.setReturnValue(new ActionResult<>(ActionResultType.PASS, itemStack));
        }
    }
}
