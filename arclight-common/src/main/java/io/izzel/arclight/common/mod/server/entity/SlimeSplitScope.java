package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ConversionParams;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import java.util.ArrayList;
import java.util.List;

public final class SlimeSplitScope implements AutoCloseable {
    private static final ThreadLocal<SlimeSplitScope> CURRENT = new ThreadLocal<>();
    private final Mob source;
    private final List<Mob> children = new ArrayList<>();
    private final List<Runnable> admissions = new ArrayList<>();
    private SlimeSplitScope(Mob source) {
        this.source = source;
        CURRENT.set(this);
    }
    public static SlimeSplitScope enter(Mob source) { return new SlimeSplitScope(source); }
    public static SlimeSplitScope current(Mob source) {
        SlimeSplitScope scope = CURRENT.get();
        return scope != null && scope.source == source ? scope : null;
    }
    public static boolean allow(Mob source, List<? extends Mob> children) {
        return !children.isEmpty() && !CraftEventFactory.callEntityTransformEvent(source,
            new ArrayList<net.minecraft.world.entity.LivingEntity>(children), EntityTransformEvent.TransformReason.SPLIT).isCancelled();
    }
    public void finish() {
        if (allow(source, children)) {
            for (Runnable admission : admissions) {
                ((WorldBridge) source.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT);
                try { admission.run(); }
                finally { ((WorldBridge) source.level()).bridge$pushAddEntityReason(null); }
            }
        }
        children.clear();
        admissions.clear();
    }
    public <T extends Mob> Conversion<T> conversion(ConversionParams.AfterConversion<T> delegate) {
        return new Conversion<>(this, delegate);
    }
    public static final class Conversion<T extends Mob> implements ConversionParams.AfterConversion<T> {
        private final SlimeSplitScope scope;
        private final ConversionParams.AfterConversion<T> delegate;
        private T child;
        private Runnable admission;
        private Conversion(SlimeSplitScope scope, ConversionParams.AfterConversion<T> delegate) {
            this.scope = scope; this.delegate = delegate;
        }
        @Override public void finalizeConversion(T child) {
            this.child = child;
            delegate.finalizeConversion(child);
        }
        public boolean defer(Mob source, net.minecraft.world.entity.Entity entity, Runnable admission) {
            if (source != scope.source || entity != child || this.admission != null) return false;
            this.admission = admission;
            return true;
        }
        public void complete(T result) {
            if (result != null && result == child && admission != null) {
                scope.children.add(result);
                scope.admissions.add(admission);
            }
        }
    }
    @Override public void close() {
        CURRENT.remove();
    }
}
