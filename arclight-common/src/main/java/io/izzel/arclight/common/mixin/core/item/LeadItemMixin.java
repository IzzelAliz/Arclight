package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.LeadItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(LeadItem.class)
public class LeadItemMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static ActionResultType bindPlayerMobs(PlayerEntity player, World worldIn, BlockPos fence) {
        LeashKnotEntity leashknotentity = null;
        boolean flag = false;
        double d0 = 7.0D;
        int i = fence.getX();
        int j = fence.getY();
        int k = fence.getZ();

        for (MobEntity mobentity : worldIn.getEntitiesWithinAABB(MobEntity.class, new AxisAlignedBB((double) i - 7.0D, (double) j - 7.0D, (double) k - 7.0D, (double) i + 7.0D, (double) j + 7.0D, (double) k + 7.0D))) {
            if (mobentity.getLeashHolder() == player) {
                if (leashknotentity == null) {
                    leashknotentity = LeashKnotEntity.create(worldIn, fence);
                    HangingPlaceEvent event = new HangingPlaceEvent((Hanging) ((EntityBridge) leashknotentity).bridge$getBukkitEntity(), player != null ? (Player) ((PlayerEntityBridge) player).bridge$getBukkitEntity() : null, CraftBlock.at(worldIn, fence), BlockFace.SELF);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        leashknotentity.remove();
                        return ActionResultType.PASS;
                    }
                }
                if (CraftEventFactory.callPlayerLeashEntityEvent(mobentity, leashknotentity, player).isCancelled()) {
                    continue;
                }
                mobentity.setLeashHolder(leashknotentity, true);
                flag = true;
            }
        }

        return flag ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }
}
