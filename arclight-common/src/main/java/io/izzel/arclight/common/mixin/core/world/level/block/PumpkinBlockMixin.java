package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.block.PlayerShearBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.function.BiConsumer;

@Mixin(PumpkinBlock.class)
public abstract class PumpkinBlockMixin {

    @Decorate(method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/PumpkinBlock;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemInstance;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"),
        require = 1, allow = 1)
    private boolean arclight$playerShearBlock(ServerLevel serverLevel, ResourceKey<LootTable> lootTable, BlockState lootState,
                                             BlockEntity blockEntity, ItemInstance tool, Entity entity, BiConsumer<ServerLevel, ItemStack> dropper,
                                             ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) throws Throwable {
        var drops = new ArrayList<org.bukkit.inventory.ItemStack>();
        boolean result = (boolean) DecorationOps.callsite().invoke(serverLevel, lootTable, lootState, blockEntity, tool, entity,
            (BiConsumer<ServerLevel, ItemStack>) (dropLevel, stack) -> drops.add(CraftItemStack.asCraftMirror(stack)));
        var event = new PlayerShearBlockEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(itemStack), CraftEquipmentSlot.getHand(hand), drops);
        if (!event.callEvent()) {
            return (boolean) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
        }
        for (org.bukkit.inventory.ItemStack drop : event.getDrops()) {
            // Keep the native carving direction, velocity and entity-admission callback.
            dropper.accept(serverLevel, CraftItemStack.asNMSCopy(drop));
        }
        return result;
    }
}
