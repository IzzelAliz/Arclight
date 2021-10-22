package io.izzel.arclight.common.mod.server.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronHooks {

    private static Entity entity;
    private static CauldronLevelChangeEvent.ChangeReason reason = CauldronLevelChangeEvent.ChangeReason.UNKNOWN;
    private static boolean lastRet = true;

    public static void setChangeReason(Entity entity, CauldronLevelChangeEvent.ChangeReason reason) {
        CauldronHooks.entity = entity;
        CauldronHooks.reason = reason;
    }

    public static void reset() {
        CauldronHooks.entity = null;
        CauldronHooks.reason = CauldronLevelChangeEvent.ChangeReason.UNKNOWN;
        CauldronHooks.lastRet = true;
    }

    public static Entity getEntity() {
        return entity;
    }

    public static CauldronLevelChangeEvent.ChangeReason getReason() {
        return reason;
    }

    public static boolean getResult() {
        return lastRet;
    }

    public static boolean changeLevel(BlockState old, Level world, BlockPos pos, BlockState state, Entity entity, CauldronLevelChangeEvent.ChangeReason reason) {
        CraftBlockState newState = CraftBlockStates.getBlockState(world, pos);
        newState.setData(state);
        CauldronLevelChangeEvent event = new CauldronLevelChangeEvent(
            CraftBlock.at(world, pos),
            (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity(),
            reason, newState
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return lastRet = false;
        } else {
            newState.update(true);
            return lastRet = true;
        }
    }
}
