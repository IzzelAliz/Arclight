package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static InteractionResult bindPlayerMobs(net.minecraft.world.entity.player.Player player, Level worldIn, BlockPos fence) {
        LeashFenceKnotEntity leashknotentity = null;
        boolean flag = false;
        double d0 = 7.0D;
        int i = fence.getX();
        int j = fence.getY();
        int k = fence.getZ();

        for (Mob mobentity : worldIn.getEntitiesOfClass(Mob.class, new AABB((double) i - 7.0D, (double) j - 7.0D, (double) k - 7.0D, (double) i + 7.0D, (double) j + 7.0D, (double) k + 7.0D))) {
            if (mobentity.getLeashHolder() == player) {
                if (leashknotentity == null) {
                    leashknotentity = LeashFenceKnotEntity.getOrCreateKnot(worldIn, fence);
                    HangingPlaceEvent event = new HangingPlaceEvent((Hanging) leashknotentity.bridge$getBukkitEntity(), player != null ? (Player) ((PlayerEntityBridge) player).bridge$getBukkitEntity() : null, CraftBlock.at(worldIn, fence), BlockFace.SELF, CraftEquipmentSlot.getHand(arclight$hand));
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        leashknotentity.discard();
                        return InteractionResult.PASS;
                    }
                }
                if (CraftEventFactory.callPlayerLeashEntityEvent(mobentity, leashknotentity, player, arclight$hand).isCancelled()) {
                    continue;
                }
                mobentity.setLeashedTo(leashknotentity, true);
                flag = true;
            }
        }

        return flag ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
