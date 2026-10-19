package io.izzel.arclight.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CapturedBlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Isolated tree proposal provider; no event, application or TreeType policy. */
public final class TreeCapture {
    private Frame current;

    private static final class Frame {
        final LinkedHashMap<BlockPos, CapturedBlockState> states = new LinkedHashMap<>();
    }

    public record Result<T>(T value, List<CapturedBlockState> states) {}

    /** Each explicit call starts a separate proposal buffer; nested ordinary Level calls share it. */
    public <T> Result<T> capture(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        Frame frame = new Frame();
        current = frame;
        try {
            T value = action.get();
            return new Result<>(value, List.copyOf(frame.states.values()));
        } finally {
            current = null;
        }
    }

    /** Only a newly owned, now-closed root exposes proposals. Joined calls expose none. */
    public record JoinResult<T>(T value, List<CapturedBlockState> states, boolean ownedRoot) {}

    /** Reuse this Level's active frame, or own a root. Explicit capture() remains isolated. */
    public <T> JoinResult<T> captureOrJoin(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        if (current != null) {
            return new JoinResult<>(action.get(), List.of(), false);
        }
        Frame root = new Frame();
        current = root;
        T value;
        try {
            value = action.get();
        } finally {
            current = null;
        }
        return new JoinResult<>(value, List.copyOf(root.states.values()), true);
    }

    /** Internal identity only; it grants no commit authority and exposes no proposals. */
    public Object currentIdentity() { return current; }

    public boolean active() { return current != null; }

    /** Invoked before native bounds/chunk writes, ordinary capture or entity event dispatch. */
    public void write(Level level, BlockPos pos, BlockState data, int flags) {
        Frame frame = Objects.requireNonNull(current, "tree capture not active");
        CapturedBlockState state = frame.states.get(pos);
        if (state == null) {
            BlockPos key = pos.immutable();
            state = CapturedBlockState.getTreeBlockState(level, key, flags);
            frame.states.put(key, state);
        }
        state.setData(data);
        state.setFlag(flags);
    }

    /** Read the current proposal before falling back to the live world. */
    public BlockState read(BlockPos pos) {
        CapturedBlockState state = current == null ? null : current.states.get(pos);
        return state == null ? null : state.getHandle();
    }
}
