package ca.spottedleaf.concurrentutil.executor.standard;

import ca.spottedleaf.concurrentutil.executor.BaseExecutor;

/**
 * Implementation of {@link BaseExecutor} which schedules tasks to be executed by a given priority.
 * @see BaseExecutor
 */
public interface PrioritisedExecutor extends BaseExecutor {

    public static enum Priority {

        /**
         * Priority value indicating the task has completed or is being completed.
         * This priority cannot be used to schedule tasks.
         */
        COMPLETING(-1),

        /**
         * Absolute highest priority, should only be used for when a task is blocking a time-critical thread.
         */
        BLOCKING(),

        /**
         * Should only be used for urgent but not time-critical tasks.
         */
        HIGHEST(),

        /**
         * Two priorities above normal.
         */
        HIGHER(),

        /**
         * One priority above normal.
         */
        HIGH(),

        /**
         * Default priority.
         */
        NORMAL(),

        /**
         * One priority below normal.
         */
        LOW(),

        /**
         * Two priorities below normal.
         */
        LOWER(),

        /**
         * Use for tasks that should eventually execute, but are not needed to.
         */
        LOWEST(),

        /**
         * Use for tasks that can be delayed indefinitely.
         */
        IDLE();

        // returns whether the priority can be scheduled
        public static boolean isValidPriority(final Priority priority) {
            return priority != null && priority != Priority.COMPLETING;
        }

        // returns the higher priority of the two
        public static Priority max(final Priority p1, final Priority p2) {
            return p1.isHigherOrEqualPriority(p2) ? p1 : p2;
        }

        // returns the lower priroity of the two
        public static Priority min(final Priority p1, final Priority p2) {
            return p1.isLowerOrEqualPriority(p2) ? p1 : p2;
        }

        public boolean isHigherOrEqualPriority(final Priority than) {
            return this.priority <= than.priority;
        }

        public boolean isHigherPriority(final Priority than) {
            return this.priority < than.priority;
        }

        public boolean isLowerOrEqualPriority(final Priority than) {
            return this.priority >= than.priority;
        }

        public boolean isLowerPriority(final Priority than) {
            return this.priority > than.priority;
        }

        public boolean isHigherOrEqualPriority(final int than) {
            return this.priority <= than;
        }

        public boolean isHigherPriority(final int than) {
            return this.priority < than;
        }

        public boolean isLowerOrEqualPriority(final int than) {
            return this.priority >= than;
        }

        public boolean isLowerPriority(final int than) {
            return this.priority > than;
        }

        public static boolean isHigherOrEqualPriority(final int priority, final int than) {
            return priority <= than;
        }

        public static boolean isHigherPriority(final int priority, final int than) {
            return priority < than;
        }

        public static boolean isLowerOrEqualPriority(final int priority, final int than) {
            return priority >= than;
        }

        public static boolean isLowerPriority(final int priority, final int than) {
            return priority > than;
        }

        static final Priority[] PRIORITIES = Priority.values();

        /** includes special priorities */
        public static final int TOTAL_PRIORITIES = PRIORITIES.length;

        public static final int TOTAL_SCHEDULABLE_PRIORITIES = TOTAL_PRIORITIES - 1;

        public static Priority getPriority(final int priority) {
            return PRIORITIES[priority + 1];
        }

        private static int priorityCounter;

        private static int nextCounter() {
            return priorityCounter++;
        }

        public final int priority;

        Priority() {
            this(nextCounter());
        }

        Priority(final int priority) {
            this.priority = priority;
        }
    }

    /**
     * Executes the next available task.
     * <p>
     *     If there is a task with priority {@link Priority#BLOCKING} available, then that such task is executed.
     * </p>
     * <p>
     *     If there is a task with priority {@link Priority#IDLE} available then that task is only executed
     *     when there are no other tasks available with a higher priority.
     * </p>
     * <p>
     *     If there are no tasks that have priority {@link Priority#BLOCKING} or {@link Priority#IDLE}, then
     *     this function will be biased to execute tasks that have higher priorities.
     * </p>
     *
     * @return {@code true} if a task was executed, {@code false} otherwise
     * @throws IllegalStateException If the current thread is not allowed to execute a task
     */
    @Override
    public boolean executeTask() throws IllegalStateException;

    /**
     * Queues or executes a task at {@link Priority#NORMAL} priority.
     * @param task The task to run.
     *
     * @throws IllegalStateException If this queue has shutdown.
     * @throws NullPointerException If the task is null
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     * associated with the parameter
     */
    public default PrioritisedTask queueRunnable(final Runnable task) {
        return this.queueRunnable(task, Priority.NORMAL);
    }

    /**
     * Queues or executes a task.
     *
     * @param task The task to run.
     * @param priority The priority for the task.
     *
     * @throws IllegalStateException If this queue has shutdown.
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     * associated with the parameter
     */
    public PrioritisedTask queueRunnable(final Runnable task, final Priority priority);

    /**
     * Creates, but does not execute or queue the task. The task must later be queued via {@link BaseTask#queue()}.
     *
     * @param task The task to run.
     *
     * @throws IllegalStateException If this queue has shutdown.
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @throws UnsupportedOperationException If this executor does not support lazily queueing tasks
     * @return The prioritised task associated with the parameters
     */
    public default PrioritisedTask createTask(final Runnable task) {
        return this.createTask(task, Priority.NORMAL);
    }

    /**
     * Creates, but does not execute or queue the task. The task must later be queued via {@link BaseTask#queue()}.
     *
     * @param task The task to run.
     * @param priority The priority for the task.
     *
     * @throws IllegalStateException If this queue has shutdown.
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @throws UnsupportedOperationException If this executor does not support lazily queueing tasks
     * @return The prioritised task associated with the parameters
     */
    public PrioritisedTask createTask(final Runnable task, final Priority priority);

    /**
     * Extension of {@link BaseTask} which adds functions
     * to retrieve and modify the task's associated priority.
     *
     * @see BaseTask
     */
    public static interface PrioritisedTask extends BaseTask {

        /**
         * Returns the current priority. Note that {@link Priority#COMPLETING} will be returned
         * if this task is completing or has completed.
         */
        public Priority getPriority();

        /**
         * Attempts to set this task's priority level to the level specified.
         *
         * @param priority Specified priority level.
         *
         * @throws IllegalArgumentException If the priority is invalid
         * @return {@code true} if successful, {@code false} if this task is completing or has completed or the queue
         * this task was scheduled on was shutdown, or if the priority was already at the specified level.
         */
        public boolean setPriority(final Priority priority);

        /**
         * Attempts to raise the priority to the priority level specified.
         *
         * @param priority Priority specified
         *
         * @throws IllegalArgumentException If the priority is invalid
         * @return {@code false} if the current task is completing, {@code true} if the priority was raised to the specified level or was already at the specified level or higher.
         */
        public boolean raisePriority(final Priority priority);

        /**
         * Attempts to lower the priority to the priority level specified.
         *
         * @param priority Priority specified
         *
         * @throws IllegalArgumentException If the priority is invalid
         * @return {@code false} if the current task is completing, {@code true} if the priority was lowered to the specified level or was already at the specified level or lower.
         */
        public boolean lowerPriority(final Priority priority);
    }
}
