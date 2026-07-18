package ca.spottedleaf.concurrentutil.executor.standard;

import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import java.lang.invoke.VarHandle;

public class DelayedPrioritisedTask {

    protected volatile int priority;
    protected static final VarHandle PRIORITY_HANDLE = ConcurrentUtil.getVarHandle(DelayedPrioritisedTask.class, "priority", int.class);

    protected static final int PRIORITY_SET = Integer.MIN_VALUE >>> 0;

    protected final int getPriorityVolatile() {
        return (int)PRIORITY_HANDLE.getVolatile((DelayedPrioritisedTask)this);
    }

    protected final int compareAndExchangePriorityVolatile(final int expect, final int update) {
        return (int)PRIORITY_HANDLE.compareAndExchange((DelayedPrioritisedTask)this, (int)expect, (int)update);
    }

    protected final int getAndOrPriorityVolatile(final int val) {
        return (int)PRIORITY_HANDLE.getAndBitwiseOr((DelayedPrioritisedTask)this, (int)val);
    }

    protected final void setPriorityPlain(final int val) {
        PRIORITY_HANDLE.set((DelayedPrioritisedTask)this, (int)val);
    }

    protected volatile PrioritisedExecutor.PrioritisedTask task;
    protected static final VarHandle TASK_HANDLE = ConcurrentUtil.getVarHandle(DelayedPrioritisedTask.class, "task", PrioritisedExecutor.PrioritisedTask.class);

    protected PrioritisedExecutor.PrioritisedTask getTaskPlain() {
        return (PrioritisedExecutor.PrioritisedTask)TASK_HANDLE.get((DelayedPrioritisedTask)this);
    }

    protected PrioritisedExecutor.PrioritisedTask getTaskVolatile() {
        return (PrioritisedExecutor.PrioritisedTask)TASK_HANDLE.getVolatile((DelayedPrioritisedTask)this);
    }

    protected final PrioritisedExecutor.PrioritisedTask compareAndExchangeTaskVolatile(final PrioritisedExecutor.PrioritisedTask expect, final PrioritisedExecutor.PrioritisedTask update) {
        return (PrioritisedExecutor.PrioritisedTask)TASK_HANDLE.compareAndExchange((DelayedPrioritisedTask)this, (PrioritisedExecutor.PrioritisedTask)expect, (PrioritisedExecutor.PrioritisedTask)update);
    }

    public DelayedPrioritisedTask(final PrioritisedExecutor.Priority priority) {
        this.setPriorityPlain(priority.priority);
    }

    // only public for debugging
    public int getPriorityInternal() {
        return this.getPriorityVolatile();
    }

    public PrioritisedExecutor.PrioritisedTask getTask() {
        return this.getTaskVolatile();
    }

    public void setTask(final PrioritisedExecutor.PrioritisedTask task) {
        int priority = this.getPriorityVolatile();

        if (this.compareAndExchangeTaskVolatile(null, task) != null) {
            throw new IllegalStateException("setTask() called twice");
        }

        int failures = 0;
        for (;;) {
            task.setPriority(PrioritisedExecutor.Priority.getPriority(priority));

            if (priority == (priority = this.compareAndExchangePriorityVolatile(priority, priority | PRIORITY_SET))) {
                return;
            }

            ++failures;
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }
        }
    }

    public PrioritisedExecutor.Priority getPriority() {
        final int priority = this.getPriorityVolatile();
        if ((priority & PRIORITY_SET) != 0) {
            return this.task.getPriority();
        }

        return PrioritisedExecutor.Priority.getPriority(priority);
    }

    public void raisePriority(final PrioritisedExecutor.Priority priority) {
        if (!PrioritisedExecutor.Priority.isValidPriority(priority)) {
            throw new IllegalArgumentException("Invalid priority " + priority);
        }

        int failures = 0;
        for (int curr = this.getPriorityVolatile();;) {
            if ((curr & PRIORITY_SET) != 0) {
                this.getTaskPlain().raisePriority(priority);
                return;
            }

            if (!priority.isLowerPriority(curr)) {
                return;
            }

            if (curr == (curr = this.compareAndExchangePriorityVolatile(curr, priority.priority))) {
                return;
            }

            // failed, retry

            ++failures;
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }
        }
    }

    public void setPriority(final PrioritisedExecutor.Priority priority) {
        if (!PrioritisedExecutor.Priority.isValidPriority(priority)) {
            throw new IllegalArgumentException("Invalid priority " + priority);
        }

        int failures = 0;
        for (int curr = this.getPriorityVolatile();;) {
            if ((curr & PRIORITY_SET) != 0) {
                this.getTaskPlain().setPriority(priority);
                return;
            }

            if (curr == (curr = this.compareAndExchangePriorityVolatile(curr, priority.priority))) {
                return;
            }

            // failed, retry

            ++failures;
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }
        }
    }

    public void lowerPriority(final PrioritisedExecutor.Priority priority) {
        if (!PrioritisedExecutor.Priority.isValidPriority(priority)) {
            throw new IllegalArgumentException("Invalid priority " + priority);
        }

        int failures = 0;
        for (int curr = this.getPriorityVolatile();;) {
            if ((curr & PRIORITY_SET) != 0) {
                this.getTaskPlain().lowerPriority(priority);
                return;
            }

            if (!priority.isHigherPriority(curr)) {
                return;
            }

            if (curr == (curr = this.compareAndExchangePriorityVolatile(curr, priority.priority))) {
                return;
            }

            // failed, retry

            ++failures;
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }
        }
    }
}
