package io.izzel.arclight.common.mod.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class ArclightCallbackExecutor implements Executor, Runnable {

    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void execute(Runnable runnable) {
        queue.add(runnable);
    }

    @Override
    public void run() {
        for (;;) {
            var poll = queue.poll();
            if (poll != null) {
                poll.run();
            } else {
                return;
            }
        }
    }
}
