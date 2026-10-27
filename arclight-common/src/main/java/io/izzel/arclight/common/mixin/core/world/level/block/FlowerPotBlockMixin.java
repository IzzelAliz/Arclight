package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotBlockMixin {

    @Decorate(method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        inject = true, require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$placeFlower(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                     Player player, InteractionHand hand, BlockHitResult hitResult) throws Throwable {
        if (level.isClientSide()) {
            return;
        }
        var event = new PlayerFlowerPotManipulateEvent(
            (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            CraftBlock.at(level, pos), CraftItemStack.asBukkitCopy(stack), true);
        if (!event.callEvent()) {
            // Paper's InventoryMenu override only forces the affected hand; other menus resync all slots.
            if (player.containerMenu instanceof InventoryMenu) {
                int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
                ((AbstractContainerMenuBridge) player.containerMenu).bridge$forceSlot(player.getInventory(), slot);
            } else {
                player.containerMenu.sendAllDataToRemote();
                // Native menu synchronization omits the offhand; Paper sends it separately.
                ((AbstractContainerMenuBridge) player.inventoryMenu).bridge$forceSlot(player.getInventory(), Inventory.SLOT_OFFHAND);
            }
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.CONSUME);
            return;
        }
    }

    @Decorate(method = "useWithoutItem(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        inject = true, require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private void arclight$takeFlower(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult,
                                    @Local(ordinal = 0) ItemStack plant) throws Throwable {
        if (level.isClientSide()) {
            return;
        }
        var event = new PlayerFlowerPotManipulateEvent(
            (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            CraftBlock.at(level, pos), CraftItemStack.asBukkitCopy(plant), false);
        if (!event.callEvent()) {
            player.containerMenu.sendAllDataToRemote();
            if (!(player.containerMenu instanceof InventoryMenu)) {
                ((AbstractContainerMenuBridge) player.inventoryMenu).bridge$forceSlot(player.getInventory(), Inventory.SLOT_OFFHAND);
            }
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }
    }
}
