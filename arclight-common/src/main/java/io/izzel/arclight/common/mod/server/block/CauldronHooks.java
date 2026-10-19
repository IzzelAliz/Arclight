package io.izzel.arclight.common.mod.server.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronHooks {

    private static final ThreadLocal<PreparedInteraction> PREPARED_INTERACTION = new ThreadLocal<>();

    public static void withPreparedInteraction(Level level, BlockPos pos, BlockState state, Runnable action) {
        PreparedInteraction previous = PREPARED_INTERACTION.get();
        PREPARED_INTERACTION.set(new PreparedInteraction(level, pos.immutable(), state));
        try {
            action.run();
        } finally {
            if (previous == null) PREPARED_INTERACTION.remove();
            else PREPARED_INTERACTION.set(previous);
        }
    }

    public static BlockState takePreparedInteraction(Level level, BlockPos pos) {
        PreparedInteraction prepared = PREPARED_INTERACTION.get();
        if (prepared == null || prepared.level != level || !prepared.pos.equals(pos)) return null;
        PREPARED_INTERACTION.remove(); // Consume before invoking the setter and any nested callbacks.
        return prepared.state;
    }

    private record PreparedInteraction(Level level, BlockPos pos, BlockState state) {}

    private static final ThreadLocal<ChangeContext> CHANGE_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<ChangeInvocation> CHANGE_INVOCATION = new ThreadLocal<>();

    // Tracks only callback identity; native eligibility checks run without event context installed.
    public static boolean runWithChangeInvocation(Entity entity, BlockPos pos, Runnable action) {
        ChangeInvocation previous = CHANGE_INVOCATION.get();
        ChangeInvocation invocation = new ChangeInvocation(entity, pos.immutable());
        CHANGE_INVOCATION.set(invocation);
        try {
            action.run();
        } finally {
            if (previous == null) {
                CHANGE_INVOCATION.remove();
            } else {
                CHANGE_INVOCATION.set(previous);
            }
        }
        return invocation.changeAttempted && invocation.accepted;
    }

    // Called only around native handleEntityOnFireInside, after isOnFire/mayInteract pass.
    public static void runWithChangeContextForCurrentInvocation(Runnable action) {
        ChangeInvocation invocation = CHANGE_INVOCATION.get();
        if (invocation == null) {
            action.run();
            return;
        }

        ChangeContext previous = CHANGE_CONTEXT.get();
        ChangeContext context = new ChangeContext(invocation.entity, invocation.pos, CauldronLevelChangeEvent.ChangeReason.EXTINGUISH);
        CHANGE_CONTEXT.set(context);
        try {
            action.run();
        } finally {
            if (previous == null) {
                CHANGE_CONTEXT.remove();
            } else {
                CHANGE_CONTEXT.set(previous);
            }
            invocation.changeAttempted |= context.changeAttempted;
            invocation.accepted |= context.accepted;
        }
    }

    private static ChangeContext currentChangeContext() {
        ChangeContext context = CHANGE_CONTEXT.get();
        return context != null && !context.changeAttempted ? context : null;
    }

    public static Entity getEntity() {
        ChangeContext context = currentChangeContext();
        return context == null ? null : context.entity;
    }

    public static CauldronLevelChangeEvent.ChangeReason getReason() {
        ChangeContext context = currentChangeContext();
        return context == null ? CauldronLevelChangeEvent.ChangeReason.UNKNOWN : context.reason;
    }

    public static boolean changeLevel(BlockState old, Level world, BlockPos pos, BlockState state, Entity entity, CauldronLevelChangeEvent.ChangeReason reason) {
        ChangeContext context = currentChangeContext();
        if (context != null) {
            // Consume before firing plugins so synchronous re-entry cannot inherit this event context.
            context.changeAttempted = true;
            entity = context.entity;
            reason = context.reason;
            pos = context.pos;
        }

        CraftBlockState newState = CraftBlockStates.getBlockState(world, pos);
        newState.setData(state);
        CauldronLevelChangeEvent event = new CauldronLevelChangeEvent(
            CraftBlock.at(world, pos),
            (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity(),
            reason, newState
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        } else {
            // Preserve the existing policy: update(false) does not mean event cancellation.
            newState.update(true);
            if (context != null) {
                context.accepted = true;
            }
            return true;
        }
    }

    private static final class ChangeInvocation {

        private final Entity entity;
        private final BlockPos pos;
        private boolean changeAttempted;
        private boolean accepted;

        private ChangeInvocation(Entity entity, BlockPos pos) {
            this.entity = entity;
            this.pos = pos;
        }
    }

    private static final class ChangeContext {

        private final Entity entity;
        private final BlockPos pos;
        private final CauldronLevelChangeEvent.ChangeReason reason;
        private boolean changeAttempted;
        private boolean accepted;

        private ChangeContext(Entity entity, BlockPos pos, CauldronLevelChangeEvent.ChangeReason reason) {
            this.entity = entity;
            this.pos = pos;
            this.reason = reason;
        }
    }
}
