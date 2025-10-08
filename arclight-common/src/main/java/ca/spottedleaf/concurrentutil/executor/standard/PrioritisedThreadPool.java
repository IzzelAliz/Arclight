package ca.spottedleaf.concurrentutil.executor.standard;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class PrioritisedThreadPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritisedThreadPool.class);

    private final PrioritisedThread[] threads;
    private final TreeSet<PrioritisedPoolExecutorImpl> queues = new TreeSet<>(PrioritisedPoolExecutorImpl.comparator());
    private final String name;
    private final long queueMaxHoldTime;

    private final ReferenceOpenHashSet<PrioritisedPoolExecutorImpl> nonShutdownQueues = new ReferenceOpenHashSet<>();
    private final ReferenceOpenHashSet<PrioritisedPoolExecutorImpl> activeQueues = new ReferenceOpenHashSet<>();

    private boolean shutdown;

    private long schedulingIdGenerator;

    private static final long DEFAULT_QUEUE_HOLD_TIME = (long)(5.0e6);

    /**
     * @param name Specified debug name of this thread pool
     * @param threads The number of threads to use
     */
    public PrioritisedThreadPool(final String name, final int threads) {
        this(name, threads, null);
    }

    /**
     * @param name Specified debug name of this thread pool
     * @param threads The number of threads to use
     * @param threadModifier Invoked for each created thread with its incremental id before starting them
     */
    public PrioritisedThreadPool(final String name, final int threads, final BiConsumer<Thread, Integer> threadModifier) {
        this(name, threads, threadModifier, DEFAULT_QUEUE_HOLD_TIME); // 5ms
    }

    /**
     * @param name Specified debug name of this thread pool
     * @param threads The number of threads to use
     * @param threadModifier Invoked for each created thread with its incremental id before starting them
     * @param queueHoldTime The maximum amount of time to spend executing tasks in a specific queue before attempting
     *                      to switch to another queue, per thread
     */
    public PrioritisedThreadPool(final String name, final int threads, final BiConsumer<Thread, Integer> threadModifier,
                                 final long queueHoldTime) { // in ns
        if (threads <= 0) {
            throw new IllegalArgumentException("Thread count must be > 0, not " + threads);
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.name = name;
        this.queueMaxHoldTime = queueHoldTime;

        this.threads = new PrioritisedThread[threads];
        for (int i = 0; i < threads; ++i) {
            this.threads[i] = new PrioritisedThread(this);

            // set default attributes
            this.threads[i].setName("Prioritised thread for pool '" + name + "' #" + i);
            this.threads[i].setUncaughtExceptionHandler((final Thread thread, final Throwable throwable) -> {
                LOGGER.error("Uncaught exception in thread " + thread.getName(), throwable);
            });

            // let thread modifier override defaults
            if (threadModifier != null) {
                threadModifier.accept(this.threads[i], Integer.valueOf(i));
            }

            // now the thread can start
            this.threads[i].start();
        }
    }

    /**
     * Returns an array representing the threads backing this thread pool.
     */
    public Thread[] getThreads() {
        return Arrays.copyOf(this.threads, this.threads.length, Thread[].class);
    }

    /**
     * Creates and returns a {@link PrioritisedPoolExecutor} to schedule tasks onto. The returned executor will execute
     * tasks on this thread pool only.
     * @param name The debug name of the executor.
     * @param minParallelism The minimum number of threads to be executing tasks from the returned executor
     *                       before threads may be allocated to other queues in this thread pool.
     * @param parallelism The maximum number of threads which may be executing tasks from the returned executor.
     * @throws IllegalStateException If this thread pool is shut down
     */
    public PrioritisedPoolExecutor createExecutor(final String name, final int minParallelism, final int parallelism) {
        synchronized (this.nonShutdownQueues) {
            if (this.shutdown) {
                throw new IllegalStateException("Queue is shutdown: " + this.toString());
            }
            final PrioritisedPoolExecutorImpl ret = new PrioritisedPoolExecutorImpl(
                    this, name,
                    Math.min(Math.max(1, parallelism), this.threads.length),
                    Math.min(Math.max(0, minParallelism), this.threads.length)
            );

            this.nonShutdownQueues.add(ret);

            synchronized (this.activeQueues) {
                this.activeQueues.add(ret);
            }

            return ret;
        }
    }

    /**
     * Prevents creation of new queues, shutdowns all non-shutdown queues if specified
     */
    public void halt(final boolean shutdownQueues) {
        synchronized (this.nonShutdownQueues) {
            this.shutdown = true;
        }
        if (shutdownQueues) {
            final ArrayList<PrioritisedPoolExecutorImpl> queuesToShutdown;
            synchronized (this.nonShutdownQueues) {
                this.shutdown = true;
                queuesToShutdown = new ArrayList<>(this.nonShutdownQueues);
            }

            for (final PrioritisedPoolExecutorImpl queue : queuesToShutdown) {
                queue.shutdown();
            }
        }


        for (final PrioritisedThread thread : this.threads) {
            // can't kill queue, queue is null
            thread.halt(false);
        }
    }

    /**
     * Waits until all threads in this pool have shutdown, or until the specified time has passed.
     * @param msToWait Maximum time to wait.
     * @return {@code false} if the maximum time passed, {@code true} otherwise.
     */
    public boolean join(final long msToWait) {
        try {
            return this.join(msToWait, false);
        } catch (final InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Waits until all threads in this pool have shutdown, or until the specified time has passed.
     * @param msToWait Maximum time to wait.
     * @return {@code false} if the maximum time passed, {@code true} otherwise.
     * @throws InterruptedException If this thread is interrupted.
     */
    public boolean joinInterruptable(final long msToWait) throws InterruptedException {
        return this.join(msToWait, true);
    }

    protected final boolean join(final long msToWait, final boolean interruptable) throws InterruptedException {
        final long nsToWait = msToWait * (1000 * 1000);
        final long start = System.nanoTime();
        final long deadline = start + nsToWait;
        boolean interrupted = false;
        try {
            for (final PrioritisedThread thread : this.threads) {
                for (;;) {
                    if (!thread.isAlive()) {
                        break;
                    }
                    final long current = System.nanoTime();
                    if (current >= deadline) {
                        return false;
                    }

                    try {
                        thread.join(Math.max(1L, (deadline - current) / (1000 * 1000)));
                    } catch (final InterruptedException ex) {
                        if (interruptable) {
                            throw ex;
                        }
                        interrupted = true;
                    }
                }
            }

            return true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Shuts down this thread pool, optionally waiting for all tasks to be executed.
     * This function will invoke {@link PrioritisedPoolExecutor#shutdown()} on all created executors on this
     * thread pool.
     * @param wait Whether to wait for tasks to be executed
     */
    public void shutdown(final boolean wait) {
        final ArrayList<PrioritisedPoolExecutorImpl> queuesToShutdown;
        synchronized (this.nonShutdownQueues) {
            this.shutdown = true;
            queuesToShutdown = new ArrayList<>(this.nonShutdownQueues);
        }

        for (final PrioritisedPoolExecutorImpl queue : queuesToShutdown) {
            queue.shutdown();
        }

        for (final PrioritisedThread thread : this.threads) {
            // none of these can be true or else NPE
            thread.close(false, false);
        }

        if (wait) {
            final ArrayList<PrioritisedPoolExecutorImpl> queues;
            synchronized (this.activeQueues) {
                queues = new ArrayList<>(this.activeQueues);
            }
            for (final PrioritisedPoolExecutorImpl queue : queues) {
                queue.waitUntilAllExecuted();
            }
        }
    }

    protected static final class PrioritisedThread extends PrioritisedQueueExecutorThread {

        protected final PrioritisedThreadPool pool;
        protected final AtomicBoolean alertedHighPriority = new AtomicBoolean();

        public PrioritisedThread(final PrioritisedThreadPool pool) {
            super(null);
            this.pool = pool;
        }

        public boolean alertHighPriorityExecutor() {
            if (!this.notifyTasks()) {
                if (!this.alertedHighPriority.get()) {
                    this.alertedHighPriority.set(true);
                }
                return false;
            }

            return true;
        }

        private boolean isAlertedHighPriority() {
            return this.alertedHighPriority.get() && this.alertedHighPriority.getAndSet(false);
        }

        @Override
        protected boolean pollTasks() {
            final PrioritisedThreadPool pool = this.pool;
            final TreeSet<PrioritisedPoolExecutorImpl> queues = this.pool.queues;

            boolean ret = false;
            for (;;) {
                if (this.halted) {
                    break;
                }
                // try to find a queue
                // note that if and ONLY IF the queues set is empty, this means there are no tasks for us to execute.
                // so we can only break when it's empty
                final PrioritisedPoolExecutorImpl queue;
                // select queue
                synchronized (queues) {
                    queue = queues.pollFirst();
                    if (queue == null) {
                        // no tasks to execute
                        break;
                    }

                    queue.schedulingId = ++pool.schedulingIdGenerator;
                    // we own this queue now, so increment the executor count
                    // do we also need to push this queue up for grabs for another executor?
                    if (++queue.concurrentExecutors < queue.maximumExecutors) {
                        // re-add to queues
                        // it's very important this is done in the same synchronised block for polling, as this prevents
                        // us from possibly later adding a queue that should not exist in the set
                        queues.add(queue);
                        queue.isQueued = true;
                    } else {
                        queue.isQueued = false;
                    }
                    // note: we cannot drain entries from the queue while holding this lock, as it will cause deadlock
                    // the queue addition holds the per-queue lock first then acquires the lock we have now, but if we
                    // try to poll now we don't hold the per queue lock but we do hold the global lock...
                }

                // parse tasks as long as we are allowed
                final long start = System.nanoTime();
                final long deadline = start + pool.queueMaxHoldTime;
                do {
                    try {
                        if (this.halted) {
                            break;
                        }
                        if (!queue.executeTask()) {
                            // no more tasks, try next queue
                            break;
                        }
                        ret = true;
                    } catch (final ThreadDeath death) {
                        throw death; // goodbye world...
                    } catch (final Throwable throwable) {
                        LOGGER.error("Exception thrown from thread '" + this.getName() + "' in queue '" + queue.toString() + "'", throwable);
                    }
                } while (!this.isAlertedHighPriority() && System.nanoTime() <= deadline);

                synchronized (queues) {
                    // decrement executors, we are no longer executing
                    if (queue.isQueued) {
                        queues.remove(queue);
                        queue.isQueued = false;
                    }
                    if (--queue.concurrentExecutors == 0 && queue.scheduledPriority == null) {
                        // reset scheduling id once the queue is empty again
                        // this will ensure empty queues are not prioritised suddenly over active queues once tasks are
                        // queued
                        queue.schedulingId = 0L;
                    }

                    // ensure the executor is queued for execution again
                    if (!queue.isHalted && queue.scheduledPriority != null) { // make sure it actually has tasks
                        queues.add(queue);
                        queue.isQueued = true;
                    }
                }
            }

            return ret;
        }
    }

    public interface PrioritisedPoolExecutor extends PrioritisedExecutor {

        /**
         * Removes this queue from the thread pool without shutting the queue down or waiting for queued tasks to be executed
         */
        public void halt();

        /**
         * Returns whether this executor is scheduled to run tasks or is running tasks, otherwise it returns whether
         * this queue is not halted and not shutdown.
         */
        public boolean isActive();
    }

    protected static final class PrioritisedPoolExecutorImpl extends PrioritisedThreadedTaskQueue implements PrioritisedPoolExecutor {

        protected final PrioritisedThreadPool pool;
        protected final long[] priorityCounts = new long[Priority.TOTAL_SCHEDULABLE_PRIORITIES];
        protected long schedulingId;
        protected int concurrentExecutors;
        protected Priority scheduledPriority;

        protected final String name;
        protected final int maximumExecutors;
        protected final int minimumExecutors;
        protected boolean isQueued;

        public PrioritisedPoolExecutorImpl(final PrioritisedThreadPool pool, final String name, final int maximumExecutors, final int minimumExecutors) {
            this.pool = pool;
            this.name = name;
            this.maximumExecutors = maximumExecutors;
            this.minimumExecutors = minimumExecutors;
        }

        public static Comparator<PrioritisedPoolExecutorImpl> comparator() {
            return (final PrioritisedPoolExecutorImpl p1, final PrioritisedPoolExecutorImpl p2) -> {
                if (p1 == p2) {
                    return 0;
                }

                final int belowMin1 = p1.minimumExecutors - p1.concurrentExecutors;
                final int belowMin2 = p2.minimumExecutors - p2.concurrentExecutors;

                // test minimum executors
                if (belowMin1 > 0 || belowMin2 > 0) {
                    // want the largest belowMin to be first
                    final int minCompare = Integer.compare(belowMin2, belowMin1);

                    if (minCompare != 0) {
                        return minCompare;
                    }
                }

                // prefer higher priority
                final int priorityCompare = p1.scheduledPriority.ordinal() - p2.scheduledPriority.ordinal();
                if (priorityCompare != 0) {
                    return priorityCompare;
                }

                // try to spread out the executors so that each can have threads executing
                final int executorCompare = p1.concurrentExecutors - p2.concurrentExecutors;
                if (executorCompare != 0) {
                    return executorCompare;
                }

                // if all else fails here we just choose whichever executor was queued first
                return Long.compare(p1.schedulingId, p2.schedulingId);
            };
        }

        private boolean isHalted;

        @Override
        public void halt() {
            final PrioritisedThreadPool pool = this.pool;
            final TreeSet<PrioritisedPoolExecutorImpl> queues = pool.queues;
            synchronized (queues) {
                if (this.isHalted) {
                    return;
                }
                this.isHalted = true;
                if (this.isQueued) {
                    queues.remove(this);
                    this.isQueued = false;
                }
            }
            synchronized (pool.nonShutdownQueues) {
                pool.nonShutdownQueues.remove(this);
            }
            synchronized (pool.activeQueues) {
                pool.activeQueues.remove(this);
            }
        }

        @Override
        public boolean isActive() {
            final PrioritisedThreadPool pool = this.pool;
            final TreeSet<PrioritisedPoolExecutorImpl> queues = pool.queues;

            synchronized (queues) {
                if (this.concurrentExecutors != 0) {
                    return true;
                }
                synchronized (pool.activeQueues) {
                    if (pool.activeQueues.contains(this)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private long totalQueuedTasks = 0L;

        @Override
        protected void priorityChange(final PrioritisedThreadedTaskQueue.PrioritisedTask task, final Priority from, final Priority to) {
            // Note: The superclass' queue lock is ALWAYS held when inside this method. So we do NOT need to do any additional synchronisation
            // for accessing this queue's state.
            final long[] priorityCounts = this.priorityCounts;
            final boolean shutdown = this.isShutdown();

            if (from == null && to == Priority.COMPLETING) {
                throw new IllegalStateException("Cannot complete task without queueing it first");
            }

            // we should only notify for queueing of tasks, not changing priorities
            final boolean shouldNotifyTasks = from == null;

            final Priority scheduledPriority = this.scheduledPriority;
            if (from != null) {
                --priorityCounts[from.priority];
            }
            if (to != Priority.COMPLETING) {
                ++priorityCounts[to.priority];
            }
            final long totalQueuedTasks;
            if (to == Priority.COMPLETING) {
                totalQueuedTasks = --this.totalQueuedTasks;
            } else if (from == null) {
                totalQueuedTasks = ++this.totalQueuedTasks;
            } else {
                totalQueuedTasks = this.totalQueuedTasks;
            }

            // find new highest priority
            int highest = Math.min(to == Priority.COMPLETING ? Priority.IDLE.priority : to.priority, scheduledPriority == null ? Priority.IDLE.priority : scheduledPriority.priority);
            int lowestPriority = priorityCounts.length; // exclusive
            for (;highest < lowestPriority; ++highest) {
                final long count = priorityCounts[highest];
                if (count < 0) {
                    throw new IllegalStateException("Priority " + highest + " has " + count + " scheduled tasks");
                }

                if (count != 0) {
                    break;
                }
            }

            final Priority newPriority;
            if (highest == lowestPriority) {
                // no tasks left
                newPriority = null;
            } else if (shutdown) {
                // whichever is lower, the actual greatest priority or simply HIGHEST
                // this is so shutdown automatically gets priority
                newPriority = Priority.getPriority(Math.min(highest, Priority.HIGHEST.priority));
            } else {
                newPriority = Priority.getPriority(highest);
            }

            final int executorsWanted;
            boolean shouldNotifyHighPriority = false;

            final PrioritisedThreadPool pool = this.pool;
            final TreeSet<PrioritisedPoolExecutorImpl> queues = pool.queues;

            synchronized (queues) {
                if (!this.isQueued) {
                    // see if we need to be queued
                    if (newPriority != null) {
                        if (this.schedulingId == 0L) {
                            this.schedulingId = ++pool.schedulingIdGenerator;
                        }
                        this.scheduledPriority = newPriority; // must be updated before queue add
                        if (!this.isHalted && this.concurrentExecutors < this.maximumExecutors) {
                            shouldNotifyHighPriority = newPriority.isHigherOrEqualPriority(Priority.HIGH);
                            queues.add(this);
                            this.isQueued = true;
                        }
                    } else {
                        // do not queue
                        this.scheduledPriority = null;
                    }
                } else {
                    // see if we need to NOT be queued
                    if (newPriority == null) {
                        queues.remove(this);
                        this.scheduledPriority = null;
                        this.isQueued = false;
                    } else if (scheduledPriority != newPriority) {
                        // if our priority changed, we need to update it - which means removing and re-adding into the queue
                        queues.remove(this);
                        // only now can we update scheduledPriority, since we are no longer in queue
                        this.scheduledPriority = newPriority;
                        queues.add(this);
                        shouldNotifyHighPriority = (scheduledPriority == null || scheduledPriority.isLowerPriority(Priority.HIGH)) && newPriority.isHigherOrEqualPriority(Priority.HIGH);
                    }
                }

                if (this.isQueued) {
                    executorsWanted = Math.min(this.maximumExecutors - this.concurrentExecutors, (int)totalQueuedTasks);
                } else {
                    executorsWanted = 0;
                }
            }

            if (newPriority == null && shutdown) {
                synchronized (pool.activeQueues) {
                    pool.activeQueues.remove(this);
                }
            }

            // Wake up the number of executors we want
            if (executorsWanted > 0 || (shouldNotifyTasks | shouldNotifyHighPriority)) {
                int notified = 0;
                for (final PrioritisedThread thread : pool.threads) {
                    if ((shouldNotifyHighPriority ? thread.alertHighPriorityExecutor() : thread.notifyTasks())
                            && (++notified >= executorsWanted)) {
                        break;
                    }
                }
            }
        }

        @Override
        public boolean shutdown() {
            final boolean ret = super.shutdown();
            if (!ret) {
                return ret;
            }

            final PrioritisedThreadPool pool = this.pool;

            // remove from active queues
            synchronized (pool.nonShutdownQueues) {
                pool.nonShutdownQueues.remove(this);
            }

            final TreeSet<PrioritisedPoolExecutorImpl> queues = pool.queues;

            // try and shift around our priority
            synchronized (queues) {
                if (this.scheduledPriority == null) {
                    // no tasks are queued, ensure we aren't in activeQueues
                    synchronized (pool.activeQueues) {
                        pool.activeQueues.remove(this);
                    }

                    return ret;
                }

                // try to set scheduled priority to HIGHEST so it drains faster

                if (this.scheduledPriority.isHigherOrEqualPriority(Priority.HIGHEST)) {
                    // already at target priority (highest or above)
                    return ret;
                }

                // shift priority to HIGHEST

                if (this.isQueued) {
                    queues.remove(this);
                    this.scheduledPriority = Priority.HIGHEST;
                    queues.add(this);
                } else {
                    this.scheduledPriority = Priority.HIGHEST;
                }
            }

            return ret;
        }
    }
}
