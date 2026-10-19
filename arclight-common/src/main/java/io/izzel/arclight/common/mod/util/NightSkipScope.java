package io.izzel.arclight.common.mod.util;

/** Carries the normal sleep tick through clock adjustment and mutation. */
public final class NightSkipScope {
    private static Object level;
    private static Object manager;
    private static Object clock;
    private static Proposal proposal;
    private static boolean cancelled;

    private NightSkipScope() {}

    @FunctionalInterface
    public interface Proposal {
        long apply(long before, long proposed);
    }

    public static final class Scope implements AutoCloseable {
        private final int stage;
        private Scope(int stage) { this.stage = stage; }
        @Override
        public void close() {
            switch (stage) {
                case 0 -> { level = null; cancelled = false; }
                case 1 -> { manager = null; clock = null; }
                case 2 -> proposal = null;
            }
        }
    }

    /** Stops the clock mutation before its update tail when Bukkit vetoes it. */
    public static final class Veto extends RuntimeException {
        private Veto() { super(null, null, false, false); }
    }

    public static Scope tick(Object world) {
        level = world;
        cancelled = false;
        return new Scope(0);
    }

    public static Scope adjustment(Object clockManager, Object worldClock) {
        manager = clockManager;
        clock = worldClock;
        return new Scope(1);
    }

    public static Scope mutation(Object clockManager, Object worldClock, Proposal action) {
        proposal = level != null && manager == clockManager && clock == worldClock ? action : null;
        return new Scope(2);
    }

    public static long propose(long before, long proposed) {
        return proposal == null ? proposed : proposal.apply(before, proposed);
    }

    public static void veto() {
        cancelled = true;
        throw new Veto();
    }

    public static boolean cancelled(Object world) {
        return level == world && cancelled;
    }
}
