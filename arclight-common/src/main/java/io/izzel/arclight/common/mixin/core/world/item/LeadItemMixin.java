package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin(LeadItem.class)
public class LeadItemMixin {

    private static InteractionHand arclight$hand;

    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/LeadItem;bindPlayerMobs(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/InteractionResult;"))
    private void arclight$captureHand(UseOnContext p_42834_, CallbackInfoReturnable<InteractionResult> cir) {
        arclight$hand = p_42834_.getHand();
    }

    @Inject(method = "useOn", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/LeadItem;bindPlayerMobs(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/InteractionResult;"))
    private void arclight$resetHand(UseOnContext p_42834_, CallbackInfoReturnable<InteractionResult> cir) {
        arclight$hand = p_42834_.getHand();
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "bindPlayerMobs", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/LeadItem;leashableInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static List<Leashable> arclight$leashEvent(Level level, BlockPos blockPos, Predicate<Leashable> predicate, net.minecraft.world.entity.player.Player player) throws Throwable {
        var leashableList = (List<Leashable>) DecorationOps.callsite().invoke(level, blockPos, predicate);
        var leashFenceKnotEntity = LeashFenceKnotEntity.getOrCreateKnot(level, blockPos);
        var hand = CraftEquipmentSlot.getHand(arclight$hand);
        var event = new HangingPlaceEvent((org.bukkit.entity.Hanging) leashFenceKnotEntity.bridge$getBukkitEntity(), player != null ? (org.bukkit.entity.Player) player.bridge$getBukkitEntity() : null, CraftBlock.at(level, blockPos), org.bukkit.block.BlockFace.SELF, hand);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            leashFenceKnotEntity.discard();
            return (List<Leashable>) DecorationOps.cancel().invoke(InteractionResult.PASS);
        }
        var newList = leashableList.stream().filter(it -> {
            if (it instanceof Entity leashed) {
                return !CraftEventFactory.callPlayerLeashEntityEvent(leashed, leashFenceKnotEntity, player, arclight$hand).isCancelled();
            }
            return true;
        }).collect(Collectors.toCollection(ArrayList::new));
        if (newList.isEmpty()) {
            leashFenceKnotEntity.discard();
        }
        return newList;
    }
}
