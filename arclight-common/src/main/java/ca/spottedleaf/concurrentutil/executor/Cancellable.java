package ca.spottedleaf.concurrentutil.executor;

/**
 * Interface specifying that something can be cancelled.
 */
public interface Cancellable {

    /**
     * Tries to cancel this task. If the task is in a stage that is too late to be cancelled, then this function
     * will return {@code false}. If the task is already cancelled, then this function returns {@code false}. Only
     * when this function successfully stops this task from being completed will it return {@code true}.
     */
    public boolean cancel();
}
