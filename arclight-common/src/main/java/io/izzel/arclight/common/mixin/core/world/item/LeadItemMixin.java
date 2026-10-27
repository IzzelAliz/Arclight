package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(LeadItem.class)
public class LeadItemMixin {

    private static InteractionHand arclight$pendingHand;

    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/LeadItem;bindPlayerMobs(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult arclight$withHand(Player player, Level level, BlockPos pos, Operation<InteractionResult> original, UseOnContext context) {
        var previous = arclight$pendingHand;
        arclight$pendingHand = context.getHand();
        try {
            return original.call(player, level, pos);
        } finally {
            arclight$pendingHand = previous;
        }
    }

    @Decorate(method = "bindPlayerMobs", inject = true, at = @At("HEAD"))
    private static void arclight$captureHand(@Local(allocate = "arclightLeadHand") InteractionHand hand) throws Throwable {
        var pending = arclight$pendingHand;
        arclight$pendingHand = null;
        hand = pending == null ? InteractionHand.MAIN_HAND : pending;
        DecorationOps.blackhole().invoke(hand);
    }

    @Decorate(method = "bindPlayerMobs", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseGet(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
    private static Object arclight$newKnotEvent(Optional<LeashFenceKnotEntity> existing, Supplier<LeashFenceKnotEntity> factory,
            @Local(ordinal = 0) Player player, @Local(ordinal = 0) Level level, @Local(ordinal = 0) BlockPos pos,
            @Local(allocate = "arclightLeadHand") InteractionHand hand) throws Throwable {
        // Match Optional.orElseGet's erased Object ABI before casting the returned knot.
        var knot = (LeashFenceKnotEntity) (Object) DecorationOps.callsite().invoke(existing, factory);
        if (existing.isEmpty()) {
            var event = new HangingPlaceEvent((org.bukkit.entity.Hanging) knot.bridge$getBukkitEntity(),
                player != null ? (org.bukkit.entity.Player) player.bridge$getBukkitEntity() : null,
                CraftBlock.at(level, pos), org.bukkit.block.BlockFace.SELF, CraftEquipmentSlot.getHand(hand));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                knot.discard();
                return DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            }
        }
        return knot;
    }

    @Decorate(method = "bindPlayerMobs", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;canHaveALeashAttachedTo(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$leashEvent(Leashable leashable, Entity knot, @Local(ordinal = 0) Player player,
            @Local(allocate = "arclightLeadHand") InteractionHand hand) throws Throwable {
        boolean allowed = (boolean) DecorationOps.callsite().invoke(leashable, knot);
        return allowed && (!(leashable instanceof Entity entity)
            || !CraftEventFactory.callPlayerLeashEntityEvent(entity, knot, player, hand).isCancelled());
    }
}
