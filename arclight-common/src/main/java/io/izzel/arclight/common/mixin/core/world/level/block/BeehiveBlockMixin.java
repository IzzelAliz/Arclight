package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures.ShearDrops;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.block.PlayerShearBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.function.BiConsumer;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @Unique
    private static ShearDrops arclight$shearDrops;


    @Decorate(method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropHoneycomb(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)V"),
        require = 1, allow = 1)
    private void arclight$playerShearBlock(ServerLevel serverLevel, ItemStack tool, BlockState lootState,
                                          BlockEntity blockEntity, Entity entity, BlockPos dropPos,
                                          ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) throws Throwable {
        var previous = arclight$shearDrops;
        var current = new ShearDrops(serverLevel, tool, lootState, blockEntity, entity, dropPos);
        arclight$shearDrops = current;
        try {
            // Keep dropHoneycomb (and its composed callsite), but defer its native drop sink.
            DecorationOps.callsite().invoke(serverLevel, tool, lootState, blockEntity, entity, dropPos);
        } finally {
            arclight$shearDrops = previous;
        }
        if (current.dropper == null) {
            return;
        }
        var event = new PlayerShearBlockEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(itemStack), CraftEquipmentSlot.getHand(hand), current.drops);
        if (!event.callEvent()) {
            // Cancel the InteractionResult host, not only the void dropHoneycomb helper.
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }
        for (org.bukkit.inventory.ItemStack drop : event.getDrops()) {
            current.dropper.accept(serverLevel, CraftItemStack.asNMSCopy(drop));
        }
    }

    @Decorate(method = "dropHoneycomb(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemInstance;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"),
        require = 1, allow = 1)
    private static boolean arclight$collectShearDrops(ServerLevel level, ResourceKey<LootTable> lootTable, BlockState state,
                                                     BlockEntity blockEntity, ItemInstance tool, Entity entity,
                                                     BiConsumer<ServerLevel, ItemStack> dropper,
                                                     ServerLevel sourceLevel, ItemStack sourceTool, BlockState sourceState,
                                                     BlockEntity sourceBlockEntity, Entity sourceEntity, BlockPos sourcePos) throws Throwable {
        var current = arclight$shearDrops;
        if (current != null && current.level == sourceLevel && current.tool == sourceTool && current.state == sourceState
            && current.blockEntity == sourceBlockEntity && current.entity == sourceEntity && current.pos.equals(sourcePos)) {
            // Claim before loot callbacks; direct helper/dispenser calls outside this use stay native.
            arclight$shearDrops = null;
            current.dropper = dropper;
            dropper = (dropLevel, stack) -> current.drops.add(CraftItemStack.asCraftMirror(stack));
        }
        return (boolean) DecorationOps.callsite().invoke(level, lootTable, state, blockEntity, tool, entity, dropper);
    }

    @Redirect(method = "angerNearbyBees", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$targetReason(Bee beeEntity, LivingEntity livingEntity) {
        ((MobBridge) beeEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
        beeEntity.setTarget(livingEntity);
    }
}
