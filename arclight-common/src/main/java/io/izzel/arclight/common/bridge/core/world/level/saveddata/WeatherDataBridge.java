package io.izzel.arclight.common.bridge.core.world.level.saveddata;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;

import java.util.Objects;

public interface WeatherDataBridge {
    void bridge$setWorld(ServerLevel world);

    static ScopeToken bridge$pushWeatherScope(WeatherData data, ServerLevel triggerWorld) {
        return ScopeState.push(data, triggerWorld);
    }

    static ServerLevel bridge$resolveWeatherWorld(WeatherData data, ServerLevel boundFallback) {
        Objects.requireNonNull(data, "data");
        for (ScopeToken token = ScopeState.CURRENT.get(); token != null; token = token.previous) {
            if (token.data == data) {
                return token.triggerWorld;
            }
        }
        return boundFallback;
    }

    final class ScopeToken implements AutoCloseable {
        private final WeatherData data;
        private final ServerLevel triggerWorld;
        private final Thread thread;
        private final ScopeToken previous;
        private boolean closed;

        private ScopeToken(WeatherData data, ServerLevel triggerWorld, ScopeToken previous) {
            this.data = data;
            this.triggerWorld = triggerWorld;
            this.previous = previous;
            this.thread = Thread.currentThread();
        }

        @Override
        public void close() {
            if (Thread.currentThread() != thread) {
                throw new IllegalStateException("weather scope closed by another thread");
            }
            if (closed || ScopeState.CURRENT.get() != this) {
                throw new IllegalStateException("weather scope requires exact active LIFO token");
            }
            if (previous == null) {
                ScopeState.CURRENT.remove();
            } else {
                ScopeState.CURRENT.set(previous);
            }
            closed = true;
        }
    }

    final class ScopeState {
        private static final ThreadLocal<ScopeToken> CURRENT = new ThreadLocal<>();

        private ScopeState() {
        }

        private static ScopeToken push(WeatherData data, ServerLevel world) {
            Objects.requireNonNull(data, "data");
            Objects.requireNonNull(world, "triggerWorld");
            ScopeToken token = new ScopeToken(data, world, CURRENT.get());
            CURRENT.set(token);
            return token;
        }
    }
}
