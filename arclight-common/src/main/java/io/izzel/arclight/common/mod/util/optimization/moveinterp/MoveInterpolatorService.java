package io.izzel.arclight.common.mod.util.optimization.moveinterp;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MoveInterpolatorService {

    private static final MoveInterpolatorService INSTANCE = new MoveInterpolatorService();

    private ScheduledExecutorService executor;
    private MoveInterpolator interpolator;

    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task);
            thread.setName("move-interpolator");
            thread.setDaemon(true);
            return thread;
        });
        interpolator = new MoveInterpolator();
        executor.scheduleAtFixedRate(interpolator, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
            interpolator = null;
        }
    }

    public static MoveInterpolatorService getInstance() {
        return INSTANCE;
    }

    public static MoveInterpolator getInterpolator() {
        return Objects.requireNonNull(INSTANCE.interpolator);
    }
}
