package ca.spottedleaf.concurrentutil.executor.standard;

import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

/**
 * Thread which will continuously drain from a specified queue.
 * <p>
 *     Note: When using this thread, queue additions to the underlying {@link #queue} are not sufficient to get this thread
 *     to execute the task. The function {@link #notifyTasks()} must be used after scheduling a task. For expected behaviour
 *     of task scheduling (thread wakes up after tasks are scheduled), use the methods provided on {@link PrioritisedExecutor}
 *     methods.
 * </p>
 */
public class PrioritisedQueueExecutorThread extends Thread implements PrioritisedExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritisedQueueExecutorThread.class);

    protected final PrioritisedExecutor queue;

    protected volatile boolean threadShutdown;

    protected volatile boolean threadParked;
    protected static final VarHandle THREAD_PARKED_HANDLE = ConcurrentUtil.getVarHandle(PrioritisedQueueExecutorThread.class, "threadParked", boolean.class);

    protected volatile boolean halted;

    protected final long spinWaitTime;

    static final long DEFAULT_SPINWAIT_TIME = (long)(0.1e6);// 0.1ms

    public PrioritisedQueueExecutorThread(final PrioritisedExecutor queue) {
        this(queue, DEFAULT_SPINWAIT_TIME); // 0.1ms
    }

    public PrioritisedQueueExecutorThread(final PrioritisedExecutor queue, final long spinWaitTime) { // in ns
        this.queue = queue;
        this.spinWaitTime = spinWaitTime;
    }

    @Override
    public void run() {
        final long spinWaitTime = this.spinWaitTime;

        main_loop:
        for (;;) {
            this.pollTasks();

            // spinwait

            final long start = System.nanoTime();

            for (;;) {
                // If we are interrupted for any reason, park() will always return immediately. Clear so that we don't needlessly use cpu in such an event.
                Thread.interrupted();
                Thread.yield();
                LockSupport.parkNanos("Spinwaiting on tasks", 10_000L); // 10us

                if (this.pollTasks()) {
                    // restart loop, found tasks
                    continue main_loop;
                }

                if (this.handleClose()) {
                    return; // we're done
                }

                if ((System.nanoTime() - start) >= spinWaitTime) {
                    break;
                }
            }

            if (this.handleClose()) {
                return;
            }

            this.setThreadParkedVolatile(true);

            // We need to parse here to avoid a race condition where a thread queues a task before we set parked to true
            // (i.e it will not notify us)
            if (this.pollTasks()) {
                this.setThreadParkedVolatile(false);
                continue;
            }

            if (this.handleClose()) {
                return;
            }

            // we don't need to check parked before sleeping, but we do need to check parked in a do-while loop
            // LockSupport.park() can fail for any reason
            while (this.getThreadParkedVolatile()) {
                Thread.interrupted();
                LockSupport.park("Waiting on tasks");
            }
        }
    }

    /**
     * Attempts to poll as many tasks as possible, returning when finished.
     * @return Whether any tasks were executed.
     */
    protected boolean pollTasks() {
        boolean ret = false;

        for (;;) {
            if (this.halted) {
                break;
            }
            try {
                if (!this.queue.executeTask()) {
                    break;
                }
                ret = true;
            } catch (final ThreadDeath death) {
                throw death; // goodbye world...
            } catch (final Throwable throwable) {
                LOGGER.error("Exception thrown from prioritized runnable task in thread '" + this.getName() + "'", throwable);
            }
        }

        return ret;
    }

    protected boolean handleClose() {
        if (this.threadShutdown) {
            this.pollTasks(); // this ensures we've emptied the queue
            return true;
        }
        return false;
    }

    /**
     * Notify this thread that a task has been added to its queue
     * @return {@code true} if this thread was waiting for tasks, {@code false} if it is executing tasks
     */
    public boolean notifyTasks() {
        if (this.getThreadParkedVolatile() && this.exchangeThreadParkedVolatile(false)) {
            LockSupport.unpark(this);
            return true;
        }
        return false;
    }

    @Override
    public PrioritisedTask createTask(final Runnable task, final Priority priority) {
        final PrioritisedTask queueTask = this.queue.createTask(task, priority);

        // need to override queue() to notify us of tasks
        return new PrioritisedTask() {
            @Override
            public Priority getPriority() {
                return queueTask.getPriority();
            }

            @Override
            public boolean setPriority(final Priority priority) {
                return queueTask.setPriority(priority);
            }

            @Override
            public boolean raisePriority(final Priority priority) {
                return queueTask.raisePriority(priority);
            }

            @Override
            public boolean lowerPriority(final Priority priority) {
                return queueTask.lowerPriority(priority);
            }

            @Override
            public boolean queue() {
                final boolean ret = queueTask.queue();
                if (ret) {
                    PrioritisedQueueExecutorThread.this.notifyTasks();
                }
                return ret;
            }

            @Override
            public boolean cancel() {
                return queueTask.cancel();
            }

            @Override
            public boolean execute() {
                return queueTask.execute();
            }
        };
    }

    @Override
    public PrioritisedTask queueRunnable(final Runnable task, final Priority priority) {
        final PrioritisedTask ret = this.queue.queueRunnable(task, priority);

        this.notifyTasks();

        return ret;
    }

    @Override
    public boolean haveAllTasksExecuted() {
        return this.queue.haveAllTasksExecuted();
    }

    @Override
    public long getTotalTasksExecuted() {
        return this.queue.getTotalTasksExecuted();
    }

    @Override
    public long getTotalTasksScheduled() {
        return this.queue.getTotalTasksScheduled();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException If the current thread is {@code this} thread, or the underlying queue throws this exception.
     */
    @Override
    public void waitUntilAllExecuted() throws IllegalStateException {
        if (Thread.currentThread() == this) {
            throw new IllegalStateException("Cannot block on our own queue");
        }
        this.queue.waitUntilAllExecuted();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException Always
     */
    @Override
    public boolean executeTask() throws IllegalStateException {
        throw new IllegalStateException();
    }

    /**
     * Closes this queue executor's queue. Optionally waits for all tasks in queue to be executed if {@code wait} is true.
     * <p>
     *     This function is MT-Safe.
     * </p>
     * @param wait If this call is to wait until the queue is empty and there are no tasks executing in the queue.
     * @param killQueue Whether to shutdown this thread's queue
     * @return whether this thread shut down the queue
     * @see #halt(boolean)
     */
    public boolean close(final boolean wait, final boolean killQueue) {
        final boolean ret = killQueue && this.queue.shutdown();
        this.threadShutdown = true;

        // force thread to respond to the shutdown
        this.setThreadParkedVolatile(false);
        LockSupport.unpark(this);

        if (wait) {
            this.waitUntilAllExecuted();
        }

        return ret;
    }


    /**
     * Causes this thread to exit without draining the queue. To ensure tasks are completed, use {@link #close(boolean, boolean)}.
     * <p>
     *     This is not safe to call with {@link #close(boolean, boolean)} if <code>wait = true</code>, in which case
     *     the waiting thread may block indefinitely.
     * </p>
     * <p>
     *     This function is MT-Safe.
     * </p>
     * @param killQueue Whether to shutdown this thread's queue
     * @see #close(boolean, boolean)
     */
    public void halt(final boolean killQueue) {
        if (killQueue) {
            this.queue.shutdown();
        }
        this.threadShutdown = true;
        this.halted = true;

        // force thread to respond to the shutdown
        this.setThreadParkedVolatile(false);
        LockSupport.unpark(this);
    }

    protected final boolean getThreadParkedVolatile() {
        return (boolean)THREAD_PARKED_HANDLE.getVolatile(this);
    }

    protected final boolean exchangeThreadParkedVolatile(final boolean value) {
        return (boolean)THREAD_PARKED_HANDLE.getAndSet(this, value);
    }

    protected final void setThreadParkedVolatile(final boolean value) {
        THREAD_PARKED_HANDLE.setVolatile(this, value);
    }
}
