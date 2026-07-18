package ca.spottedleaf.concurrentutil.executor.standard;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

public class PrioritisedThreadedTaskQueue implements PrioritisedExecutor {

    protected final ArrayDeque<PrioritisedTask>[] queues = new ArrayDeque[Priority.TOTAL_SCHEDULABLE_PRIORITIES]; {
        for (int i = 0; i < Priority.TOTAL_SCHEDULABLE_PRIORITIES; ++i) {
            this.queues[i] = new ArrayDeque<>();
        }
    }

    // Use AtomicLong to separate from the queue field, we don't want false sharing here.
    protected final AtomicLong totalScheduledTasks = new AtomicLong();
    protected final AtomicLong totalCompletedTasks = new AtomicLong();

    // this is here to prevent failures to queue stalling flush() calls (as the schedule calls would increment totalScheduledTasks without this check)
    protected volatile boolean hasShutdown;

    protected long taskIdGenerator = 0;

    @Override
    public PrioritisedExecutor.PrioritisedTask queueRunnable(final Runnable task, final Priority priority) throws IllegalStateException, IllegalArgumentException {
        if (!Priority.isValidPriority(priority)) {
            throw new IllegalArgumentException("Priority " + priority + " is invalid");
        }
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        if (this.hasShutdown) {
            // prevent us from stalling flush() calls by incrementing scheduled tasks when we really didn't schedule something
            throw new IllegalStateException("Queue has shutdown");
        }

        final PrioritisedTask ret;

        synchronized (this.queues) {
            if (this.hasShutdown) {
                throw new IllegalStateException("Queue has shutdown");
            }
            this.getAndAddTotalScheduledTasksVolatile(1L);

            ret = new PrioritisedTask(this.taskIdGenerator++, task, priority, this);

            this.queues[ret.priority.priority].add(ret);

            // call priority change callback (note: only after we successfully queue!)
            this.priorityChange(ret, null, priority);
        }

        return ret;
    }

    @Override
    public PrioritisedExecutor.PrioritisedTask createTask(final Runnable task, final Priority priority) {
        if (!Priority.isValidPriority(priority)) {
            throw new IllegalArgumentException("Priority " + priority + " is invalid");
        }
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        return new PrioritisedTask(task, priority, this);
    }

    @Override
    public long getTotalTasksScheduled() {
        return this.totalScheduledTasks.get();
    }

    @Override
    public long getTotalTasksExecuted() {
        return this.totalCompletedTasks.get();
    }

    // callback method for subclasses to override
    // from is null when a task is immediately created
    protected void priorityChange(final PrioritisedTask task, final Priority from, final Priority to) {}

    /**
     * Polls the highest priority task currently available. {@code null} if none. This will mark the
     * returned task as completed.
     */
    protected PrioritisedTask poll() {
        return this.poll(Priority.IDLE);
    }

    protected PrioritisedTask poll(final Priority minPriority) {
        final ArrayDeque<PrioritisedTask>[] queues = this.queues;
        synchronized (queues) {
            final int max = minPriority.priority;
            for (int i = 0; i <= max; ++i) {
                final ArrayDeque<PrioritisedTask> queue = queues[i];
                PrioritisedTask task;
                while ((task = queue.pollFirst()) != null) {
                    if (task.trySetCompleting(i)) {
                        return task;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Polls and executes the highest priority task currently available. Exceptions thrown during task execution will
     * be rethrown.
     * @return {@code true} if a task was executed, {@code false} otherwise.
     */
    @Override
    public boolean executeTask() {
        final PrioritisedTask task = this.poll();

        if (task != null) {
            task.executeInternal();
            return true;
        }

        return false;
    }

    @Override
    public boolean shutdown() {
        synchronized (this.queues) {
            if (this.hasShutdown) {
                return false;
            }
            this.hasShutdown = true;
        }
        return true;
    }

    @Override
    public boolean isShutdown() {
        return this.hasShutdown;
    }

    /* totalScheduledTasks */

    protected final long getTotalScheduledTasksVolatile() {
        return this.totalScheduledTasks.get();
    }

    protected final long getAndAddTotalScheduledTasksVolatile(final long value) {
        return this.totalScheduledTasks.getAndAdd(value);
    }

    /* totalCompletedTasks */

    protected final long getTotalCompletedTasksVolatile() {
        return this.totalCompletedTasks.get();
    }

    protected final long getAndAddTotalCompletedTasksVolatile(final long value) {
        return this.totalCompletedTasks.getAndAdd(value);
    }

    protected static final class PrioritisedTask implements PrioritisedExecutor.PrioritisedTask {
        protected final PrioritisedThreadedTaskQueue queue;
        protected long id;
        protected static final long NOT_SCHEDULED_ID = -1L;

        protected Runnable runnable;
        protected volatile Priority priority;

        protected PrioritisedTask(final long id, final Runnable runnable, final Priority priority, final PrioritisedThreadedTaskQueue queue) {
            if (!Priority.isValidPriority(priority)) {
                throw new IllegalArgumentException("Invalid priority " + priority);
            }

            this.priority = priority;
            this.runnable = runnable;
            this.queue = queue;
            this.id = id;
        }

        protected PrioritisedTask(final Runnable runnable, final Priority priority, final PrioritisedThreadedTaskQueue queue) {
            if (!Priority.isValidPriority(priority)) {
                throw new IllegalArgumentException("Invalid priority " + priority);
            }

            this.priority = priority;
            this.runnable = runnable;
            this.queue = queue;
            this.id = NOT_SCHEDULED_ID;
        }

        @Override
        public boolean queue() {
            if (this.queue.hasShutdown) {
                throw new IllegalStateException("Queue has shutdown");
            }

            synchronized (this.queue.queues) {
                if (this.queue.hasShutdown) {
                    throw new IllegalStateException("Queue has shutdown");
                }

                final Priority priority = this.priority;
                if (priority == Priority.COMPLETING) {
                    return false;
                }

                if (this.id != NOT_SCHEDULED_ID) {
                    return false;
                }

                this.queue.getAndAddTotalScheduledTasksVolatile(1L);
                this.id = this.queue.taskIdGenerator++;
                this.queue.queues[priority.priority].add(this);

                this.queue.priorityChange(this, null, priority);

                return true;
            }
        }

        protected boolean trySetCompleting(final int minPriority) {
            final Priority oldPriority = this.priority;
            if (oldPriority != Priority.COMPLETING && oldPriority.isHigherOrEqualPriority(minPriority)) {
                this.priority = Priority.COMPLETING;
                if (this.id != NOT_SCHEDULED_ID) {
                    this.queue.priorityChange(this, oldPriority, Priority.COMPLETING);
                }
                return true;
            }

            return false;
        }

        @Override
        public Priority getPriority() {
            return this.priority;
        }

        @Override
        public boolean setPriority(final Priority priority) {
            if (!Priority.isValidPriority(priority)) {
                throw new IllegalArgumentException("Invalid priority " + priority);
            }
            synchronized (this.queue.queues) {
                final Priority curr = this.priority;

                if (curr == Priority.COMPLETING) {
                    return false;
                }

                if (curr == priority) {
                    return true;
                }

                this.priority = priority;
                if (this.id != NOT_SCHEDULED_ID) {
                    this.queue.queues[priority.priority].add(this);

                    // call priority change callback
                    this.queue.priorityChange(this, curr, priority);
                }
            }

            return true;
        }

        @Override
        public boolean raisePriority(final Priority priority) {
            if (!Priority.isValidPriority(priority)) {
                throw new IllegalArgumentException("Invalid priority " + priority);
            }

            synchronized (this.queue.queues) {
                final Priority curr = this.priority;

                if (curr == Priority.COMPLETING) {
                    return false;
                }

                if (curr.isHigherOrEqualPriority(priority)) {
                    return true;
                }

                this.priority = priority;
                if (this.id != NOT_SCHEDULED_ID) {
                    this.queue.queues[priority.priority].add(this);

                    // call priority change callback
                    this.queue.priorityChange(this, curr, priority);
                }
            }

            return true;
        }

        @Override
        public boolean lowerPriority(final Priority priority) {
            if (!Priority.isValidPriority(priority)) {
                throw new IllegalArgumentException("Invalid priority " + priority);
            }

            synchronized (this.queue.queues) {
                final Priority curr = this.priority;

                if (curr == Priority.COMPLETING) {
                    return false;
                }

                if (curr.isLowerOrEqualPriority(priority)) {
                    return true;
                }

                this.priority = priority;
                if (this.id != NOT_SCHEDULED_ID) {
                    this.queue.queues[priority.priority].add(this);

                    // call priority change callback
                    this.queue.priorityChange(this, curr, priority);
                }
            }

            return true;
        }

        @Override
        public boolean cancel() {
            final long id;
            synchronized (this.queue.queues) {
                final Priority oldPriority = this.priority;
                if (oldPriority == Priority.COMPLETING) {
                    return false;
                }

                this.priority = Priority.COMPLETING;
                // call priority change callback
                if ((id = this.id) != NOT_SCHEDULED_ID) {
                    this.queue.priorityChange(this, oldPriority, Priority.COMPLETING);
                }
            }
            this.runnable = null;
            if (id != NOT_SCHEDULED_ID) {
                this.queue.getAndAddTotalCompletedTasksVolatile(1L);
            }
            return true;
        }

        protected void executeInternal() {
            try {
                final Runnable execute = this.runnable;
                this.runnable = null;
                execute.run();
            } finally {
                if (this.id != NOT_SCHEDULED_ID) {
                    this.queue.getAndAddTotalCompletedTasksVolatile(1L);
                }
            }
        }

        @Override
        public boolean execute() {
            synchronized (this.queue.queues) {
                final Priority oldPriority = this.priority;
                if (oldPriority == Priority.COMPLETING) {
                    return false;
                }

                this.priority = Priority.COMPLETING;
                // call priority change callback
                if (this.id != NOT_SCHEDULED_ID) {
                    this.queue.priorityChange(this, oldPriority, Priority.COMPLETING);
                }
            }

            this.executeInternal();
            return true;
        }
    }
}
