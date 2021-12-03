package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
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

    @Inject(method = "useOn", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/HangingEntity;playPlacementSound()V"))
    public void arclight$hangingPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, BlockPos blockPos, Direction direction, BlockPos blockPos1, net.minecraft.world.entity.player.Player playerEntity, ItemStack itemStack, Level world, HangingEntity hangingEntity) {
        if (!DistValidate.isValid(context)) return;
        Player who = (context.getPlayer() == null) ? null : (Player) ((PlayerEntityBridge) context.getPlayer()).bridge$getBukkitEntity();
        Block blockClicked = CraftBlock.at(world, blockPos);
        BlockFace blockFace = CraftBlock.notchToBlockFace(direction);

        HangingPlaceEvent event = new HangingPlaceEvent((Hanging) ((EntityBridge) hangingEntity).bridge$getBukkitEntity(), who, blockClicked, blockFace, CraftItemStack.asBukkitCopy(itemStack));
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
