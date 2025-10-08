package ca.spottedleaf.concurrentutil.scheduler;

import ca.spottedleaf.concurrentutil.set.LinkedSortedSet;
import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.concurrentutil.util.TimeUtil;
import java.lang.invoke.VarHandle;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

public class SchedulerThreadPool {

    public static final long DEADLINE_NOT_SET = Long.MIN_VALUE;

    private static final Comparator<SchedulableTick> TICK_COMPARATOR_BY_TIME = (final SchedulableTick t1, final SchedulableTick t2) -> {
        final int timeCompare = TimeUtil.compareTimes(t1.scheduledStart, t2.scheduledStart);
        if (timeCompare != 0) {
            return timeCompare;
        }

        return Long.compare(t1.id, t2.id);
    };

    private final TickThreadRunner[] runners;
    private final Thread[] threads;
    private final LinkedSortedSet<SchedulableTick> awaiting = new LinkedSortedSet<>(TICK_COMPARATOR_BY_TIME);
    private final PriorityQueue<SchedulableTick> queued = new PriorityQueue<>(TICK_COMPARATOR_BY_TIME);
    private final BitSet idleThreads;

    private final Object scheduleLock = new Object();

    private volatile boolean halted;

    /**
     * Creates, but does not start, a scheduler thread pool with the specified number of threads
     * created using the specified thread factory.
     * @param threads Specified number of threads
     * @param threadFactory Specified thread factory
     * @see #start()
     */
    public SchedulerThreadPool(final int threads, final ThreadFactory threadFactory) {
        final BitSet idleThreads = new BitSet(threads);
        for (int i = 0; i < threads; ++i) {
            idleThreads.set(i);
        }
        this.idleThreads = idleThreads;

        final TickThreadRunner[] runners = new TickThreadRunner[threads];
        final Thread[] t = new Thread[threads];
        for (int i = 0; i < threads; ++i) {
            runners[i] = new TickThreadRunner(i, this);
            t[i] = threadFactory.newThread(runners[i]);
        }

        this.threads = t;
        this.runners = runners;
    }

    /**
     * Starts the threads in this pool.
     */
    public void start() {
        for (final Thread thread : this.threads) {
            thread.start();
        }
    }

    /**
     * Attempts to prevent further execution of tasks, optionally waiting for the scheduler threads to die.
     *
     * @param sync Whether to wait for the scheduler threads to die.
     * @param maxWaitNS The maximum time, in ns, to wait for the scheduler threads to die.
     * @return {@code true} if sync was false, or if sync was true and the scheduler threads died before the timeout.
     *          Otherwise, returns {@code false} if the time elapsed exceeded the maximum wait time.
     */
    public boolean halt(final boolean sync, final long maxWaitNS) {
        this.halted = true;
        for (final Thread thread : this.threads) {
            // force response to halt
            LockSupport.unpark(thread);
        }
        final long time = System.nanoTime();
        if (sync) {
            // start at 10 * 0.5ms -> 5ms
            for (long failures = 9L;; failures = ConcurrentUtil.linearLongBackoff(failures, 500_000L, 50_000_000L)) {
                boolean allDead = true;
                for (final Thread thread : this.threads) {
                    if (thread.isAlive()) {
                        allDead = false;
                        break;
                    }
                }
                if (allDead) {
                    return true;
                }
                if ((System.nanoTime() - time) >= maxWaitNS) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns an array of the underlying scheduling threads.
     */
    public Thread[] getThreads() {
        return this.threads.clone();
    }

    private void insertFresh(final SchedulableTick task) {
        final TickThreadRunner[] runners = this.runners;

        final int firstIdleThread = this.idleThreads.nextSetBit(0);

        if (firstIdleThread != -1) {
            // push to idle thread
            this.idleThreads.clear(firstIdleThread);
            final TickThreadRunner runner = runners[firstIdleThread];
            task.awaitingLink = this.awaiting.addLast(task);
            runner.acceptTask(task);
            return;
        }

        // try to replace the last awaiting task
        final SchedulableTick last = this.awaiting.last();

        if (last != null && TICK_COMPARATOR_BY_TIME.compare(task, last) < 0) {
            // need to replace the last task
            this.awaiting.pollLast();
            last.awaitingLink = null;
            task.awaitingLink = this.awaiting.addLast(task);
            // need to add task to queue to be picked up later
            this.queued.add(last);

            final TickThreadRunner runner = last.ownedBy;
            runner.replaceTask(task);

            return;
        }

        // add to queue, will be picked up later
        this.queued.add(task);
    }

    private void takeTask(final TickThreadRunner runner, final SchedulableTick tick) {
        if (!this.awaiting.remove(tick.awaitingLink)) {
            throw new IllegalStateException("Task is not in awaiting");
        }
        tick.awaitingLink = null;
    }

    private SchedulableTick returnTask(final TickThreadRunner runner, final SchedulableTick reschedule) {
        if (reschedule != null) {
            this.queued.add(reschedule);
        }
        final SchedulableTick ret = this.queued.poll();
        if (ret == null) {
            this.idleThreads.set(runner.id);
        } else {
            ret.awaitingLink = this.awaiting.addLast(ret);
        }

        return ret;
    }

    /**
     * Schedules the specified task to be executed on this thread pool.
     * @param task Specified task
     * @throws IllegalStateException If the task is already scheduled
     * @see SchedulableTick
     */
    public void schedule(final SchedulableTick task) {
        synchronized (this.scheduleLock) {
            if (!task.tryMarkScheduled()) {
                throw new IllegalStateException("Task " + task + " is already scheduled or cancelled");
            }

            task.schedulerOwnedBy = this;

            this.insertFresh(task);
        }
    }

    /**
     * Updates the tasks scheduled start to the maximum of its current scheduled start and the specified
     * new start. If the task is not scheduled, returns {@code false}. Otherwise, returns whether the
     * scheduled start was updated. Undefined behavior of the specified task is scheduled in another executor.
     * @param task Specified task
     * @param newStart Specified new start
     */
    public boolean updateTickStartToMax(final SchedulableTick task, final long newStart) {
        synchronized (this.scheduleLock) {
            if (TimeUtil.compareTimes(newStart, task.getScheduledStart()) <= 0) {
                return false;
            }
            if (this.queued.remove(task)) {
                task.setScheduledStart(newStart);
                this.queued.add(task);
                return true;
            }
            if (task.awaitingLink != null) {
                this.awaiting.remove(task.awaitingLink);
                task.awaitingLink = null;

                // re-queue task
                task.setScheduledStart(newStart);
                this.queued.add(task);

                // now we need to replace the task the runner was waiting for
                final TickThreadRunner runner = task.ownedBy;
                final SchedulableTick replace = this.queued.poll();

                // replace cannot be null, since we have added a task to queued
                if (replace != task) {
                    runner.replaceTask(replace);
                }

                return true;
            }

            return false;
        }
    }

    /**
     * Returns {@code null} if the task is not scheduled, returns {@code TRUE} if the task was cancelled
     * and was queued to execute, returns {@code FALSE} if the task was cancelled but was executing.
     */
    public Boolean tryRetire(final SchedulableTick task) {
        if (task.schedulerOwnedBy != this) {
            return null;
        }

        synchronized (this.scheduleLock) {
            if (this.queued.remove(task)) {
                // cancelled, and no runner owns it - so return
                return Boolean.TRUE;
            }
            if (task.awaitingLink != null) {
                this.awaiting.remove(task.awaitingLink);
                task.awaitingLink = null;
                // here we need to replace the task the runner was waiting for
                final TickThreadRunner runner = task.ownedBy;
                final SchedulableTick replace = this.queued.poll();

                if (replace == null) {
                    // nothing to replace with, set to idle
                    this.idleThreads.set(runner.id);
                    runner.forceIdle();
                } else {
                    runner.replaceTask(replace);
                }

                return Boolean.TRUE;
            }

            // could not find it in queue
            return task.tryMarkCancelled() ? Boolean.FALSE : null;
        }
    }

    /**
     * Indicates that intermediate tasks are available to be executed by the task.
     * <p>
     * Note: currently a no-op
     * </p>
     * @param task The specified task
     * @see SchedulableTick
     */
    public void notifyTasks(final SchedulableTick task) {
        // Not implemented
    }

    /**
     * Represents a tickable task that can be scheduled into a {@link SchedulerThreadPool}.
     * <p>
     * A tickable task is expected to run on a fixed interval, which is determined by
     * the {@link SchedulerThreadPool}.
     * </p>
     * <p>
     * A tickable task can have intermediate tasks that can be executed before its tick method is ran. Instead of
     * the {@link SchedulerThreadPool} parking in-between ticks, the scheduler will instead drain
     * intermediate tasks from scheduled tasks. The parsing of intermediate tasks allows the scheduler to take
     * advantage of downtime to reduce the intermediate task load from tasks once they begin ticking.
     * </p>
     * <p>
     * It is guaranteed that {@link #runTick()} and {@link #runTasks(BooleanSupplier)} are never
     * invoked in parallel.
     * It is required that when intermediate tasks are scheduled, that {@link SchedulerThreadPool#notifyTasks(SchedulableTick)}
     * is invoked for any scheduled task - otherwise, {@link #runTasks(BooleanSupplier)} may not be invoked to
     * parse intermediate tasks.
     * </p>
     */
    public static abstract class SchedulableTick {
        private static final AtomicLong ID_GENERATOR = new AtomicLong();
        public final long id = ID_GENERATOR.getAndIncrement();

        private static final int SCHEDULE_STATE_NOT_SCHEDULED = 0;
        private static final int SCHEDULE_STATE_SCHEDULED = 1;
        private static final int SCHEDULE_STATE_CANCELLED = 2;

        private final AtomicInteger scheduled = new AtomicInteger();
        private SchedulerThreadPool schedulerOwnedBy;
        private long scheduledStart = DEADLINE_NOT_SET;
        private TickThreadRunner ownedBy;

        private LinkedSortedSet.Link<SchedulableTick> awaitingLink;

        private boolean tryMarkScheduled() {
            return this.scheduled.compareAndSet(SCHEDULE_STATE_NOT_SCHEDULED, SCHEDULE_STATE_SCHEDULED);
        }

        private boolean tryMarkCancelled() {
            return this.scheduled.compareAndSet(SCHEDULE_STATE_SCHEDULED, SCHEDULE_STATE_CANCELLED);
        }

        private boolean isScheduled() {
            return this.scheduled.get() == SCHEDULE_STATE_SCHEDULED;
        }

        protected final long getScheduledStart() {
            return this.scheduledStart;
        }

        /**
         * If this task is scheduled, then this may only be invoked during {@link #runTick()},
         * and {@link #runTasks(BooleanSupplier)}
         */
        protected final void setScheduledStart(final long value) {
            this.scheduledStart = value;
        }

        /**
         * Executes the tick.
         * <p>
         * It is the callee's responsibility to invoke {@link #setScheduledStart(long)} to adjust the start of
         * the next tick.
         * </p>
         * @return {@code true} if the task should continue to be scheduled, {@code false} otherwise.
         */
        public abstract boolean runTick();

        /**
         * Returns whether this task has any intermediate tasks that can be executed.
         */
        public abstract boolean hasTasks();

        /**
         * Returns {@code null} if this task should not be scheduled, otherwise returns
         * {@code Boolean.TRUE} if there are more intermediate tasks to execute and
         * {@code Boolean.FALSE} if there are no more intermediate tasks to execute.
         */
        public abstract Boolean runTasks(final BooleanSupplier canContinue);

        @Override
        public String toString() {
            return "SchedulableTick:{" +
                    "class=" + this.getClass().getName() + "," +
                    "scheduled_state=" + this.scheduled.get() + ","
                    + "}";
        }
    }

    private static final class TickThreadRunner implements Runnable {

        /**
         * There are no tasks in this thread's runqueue, so it is parked.
         * <p>
         * stateTarget = null
         * </p>
         */
        private static final int STATE_IDLE = 0;

        /**
         * The runner is waiting to tick a task, as it has no intermediate tasks to execute.
         * <p>
         * stateTarget = the task awaiting tick
         * </p>
         */
        private static final int STATE_AWAITING_TICK = 1;

        /**
         * The runner is executing a tick for one of the tasks that was in its runqueue.
         * <p>
         * stateTarget = the task being ticked
         * </p>
         */
        private static final int STATE_EXECUTING_TICK = 2;

        public final int id;
        public final SchedulerThreadPool scheduler;

        private volatile Thread thread;
        private volatile TickThreadRunnerState state = new TickThreadRunnerState(null, STATE_IDLE);
        private static final VarHandle STATE_HANDLE = ConcurrentUtil.getVarHandle(TickThreadRunner.class, "state", TickThreadRunnerState.class);

        private void setStatePlain(final TickThreadRunnerState state) {
            STATE_HANDLE.set(this, state);
        }

        private void setStateOpaque(final TickThreadRunnerState state) {
            STATE_HANDLE.setOpaque(this, state);
        }

        private void setStateVolatile(final TickThreadRunnerState state) {
            STATE_HANDLE.setVolatile(this, state);
        }

        private static record TickThreadRunnerState(SchedulableTick stateTarget, int state) {}

        public TickThreadRunner(final int id, final SchedulerThreadPool scheduler) {
            this.id = id;
            this.scheduler = scheduler;
        }

        private Thread getRunnerThread() {
            return this.thread;
        }

        private void acceptTask(final SchedulableTick task) {
            if (task.ownedBy != null) {
                throw new IllegalStateException("Already owned by another runner");
            }
            task.ownedBy = this;
            final TickThreadRunnerState state = this.state;
            if (state.state != STATE_IDLE) {
                throw new IllegalStateException("Cannot accept task in state " + state);
            }
            this.setStateVolatile(new TickThreadRunnerState(task, STATE_AWAITING_TICK));
            LockSupport.unpark(this.getRunnerThread());
        }

        private void replaceTask(final SchedulableTick task) {
            final TickThreadRunnerState state = this.state;
            if (state.state != STATE_AWAITING_TICK) {
                throw new IllegalStateException("Cannot replace task in state " + state);
            }
            if (task.ownedBy != null) {
                throw new IllegalStateException("Already owned by another runner");
            }
            task.ownedBy = this;

            state.stateTarget.ownedBy = null;

            this.setStateVolatile(new TickThreadRunnerState(task, STATE_AWAITING_TICK));
            LockSupport.unpark(this.getRunnerThread());
        }

        private void forceIdle() {
            final TickThreadRunnerState state = this.state;
            if (state.state != STATE_AWAITING_TICK) {
                throw new IllegalStateException("Cannot replace task in state " + state);
            }
            state.stateTarget.ownedBy = null;
            this.setStateOpaque(new TickThreadRunnerState(null, STATE_IDLE));
            // no need to unpark
        }

        private boolean takeTask(final TickThreadRunnerState state, final SchedulableTick task) {
            synchronized (this.scheduler.scheduleLock) {
                if (this.state != state) {
                    return false;
                }
                this.setStatePlain(new TickThreadRunnerState(task, STATE_EXECUTING_TICK));
                this.scheduler.takeTask(this, task);
                return true;
            }
        }

        private void returnTask(final SchedulableTick task, final boolean reschedule) {
            synchronized (this.scheduler.scheduleLock) {
                task.ownedBy = null;

                final SchedulableTick newWait = this.scheduler.returnTask(this, reschedule && task.isScheduled() ? task : null);
                if (newWait == null) {
                    this.setStatePlain(new TickThreadRunnerState(null, STATE_IDLE));
                } else {
                    if (newWait.ownedBy != null) {
                        throw new IllegalStateException("Already owned by another runner");
                    }
                    newWait.ownedBy = this;
                    this.setStatePlain(new TickThreadRunnerState(newWait, STATE_AWAITING_TICK));
                }
            }
        }

        @Override
        public void run() {
            this.thread = Thread.currentThread();

            main_state_loop:
            for (;;) {
                final TickThreadRunnerState startState = this.state;
                final int startStateType = startState.state;
                final SchedulableTick startStateTask =  startState.stateTarget;

                if (this.scheduler.halted) {
                    return;
                }

                switch (startStateType) {
                    case STATE_IDLE: {
                        while (this.state.state == STATE_IDLE) {
                            LockSupport.park();
                            if (this.scheduler.halted) {
                                return;
                            }
                        }
                        continue main_state_loop;
                    }

                    case STATE_AWAITING_TICK: {
                        final long deadline = startStateTask.getScheduledStart();
                        for (;;) {
                            if (this.state != startState) {
                                continue main_state_loop;
                            }
                            final long diff = deadline - System.nanoTime();
                            if (diff <= 0L) {
                                break;
                            }
                            LockSupport.parkNanos(startState, diff);
                            if (this.scheduler.halted) {
                                return;
                            }
                        }

                        if (!this.takeTask(startState, startStateTask)) {
                            continue main_state_loop;
                        }

                        // TODO exception handling
                        final boolean reschedule = startStateTask.runTick();

                        this.returnTask(startStateTask, reschedule);

                        continue main_state_loop;
                    }

                    case STATE_EXECUTING_TICK: {
                        throw new IllegalStateException("Tick execution must be set by runner thread, not by any other thread");
                    }

                    default: {
                        throw new IllegalStateException("Unknown state: " + startState);
                    }
                }
            }
        }
    }
}
