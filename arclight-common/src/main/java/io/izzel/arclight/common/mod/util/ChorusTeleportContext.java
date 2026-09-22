package io.izzel.arclight.common.mod.util;

import net.minecraft.world.entity.LivingEntity;

/**
 * A callsite-owned scope for one C0088 chorus-fruit randomTeleport invocation.
 *
 * <p>Pending scopes exist only while TeleportRandomlyConsumeEffect#apply invokes
 * its exact randomTeleport callsite. Each randomTeleport invocation installs an
 * active value, including null: a nested generic invocation therefore shadows an
 * outer chorus scope instead of inheriting its cause or cancellation result.</p>
 */
public final class ChorusTeleportContext {

    private static final ThreadLocal<Scope> PENDING = new ThreadLocal<>();
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

    private ChorusTeleportContext() {
    }

    public static Scope push(LivingEntity entity) {
        Scope previous = PENDING.get();
        Scope scope = new Scope(entity, previous);
        PENDING.set(scope);
        return scope;
    }

    public static void pop(Scope scope) {
        if (PENDING.get() != scope) {
            throw new IllegalStateException("C0088 chorus scope closed out of order");
        }
        if (scope.previous == null) {
            PENDING.remove();
        } else {
            PENDING.set(scope.previous);
        }
    }

    /** Claims only the first matching randomTeleport immediately reached from the callsite. */
    public static Frame enter(LivingEntity entity) {
        Scope previous = ACTIVE.get();
        Scope scope = PENDING.get();
        scope = scope != null && !scope.claimed && scope.entity == entity ? scope : null;
        if (scope != null) {
            scope.claimed = true;
        }
        ACTIVE.set(scope);
        return new Frame(previous);
    }

    public static void exit(Frame frame) {
        if (frame.previous == null) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(frame.previous);
        }
    }

    public static Scope current() {
        return ACTIVE.get();
    }

    public static final class Scope {

        private final LivingEntity entity;
        private final Scope previous;
        private boolean claimed;
        private boolean cancelled;

        private Scope(LivingEntity entity, Scope previous) {
            this.entity = entity;
            this.previous = previous;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean cancelled() {
            return this.cancelled;
        }
    }

    public static final class Frame {

        private final Scope previous;

        private Frame(Scope previous) {
            this.previous = previous;
        }
    }
}
