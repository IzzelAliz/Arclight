package io.izzel.arclight.common.mod.util;

import net.minecraft.util.FutureChain;
import net.minecraft.util.TaskChainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Completion notifications for command-message resolvers, not ordinary chat consumers. */
public final class CommandMessageTask<T> implements Consumer<T> {

    private final CompletableFuture<?> resolution;
    private final Consumer<T> action;

    private CommandMessageTask(CompletableFuture<?> resolution, Consumer<T> action) {
        this.resolution = resolution;
        this.action = action;
    }

    public static void observeFailure(Consumer<?> task, CompletableFuture<?> input) {
        if (task instanceof CompletableFuture<?> resolution) {
            input.whenComplete((ignored, failure) -> {
                if (failure != null) {
                    fail(resolution, failure);
                }
            });
        }
    }

    public static <T> void append(TaskChainer chainer, CompletableFuture<T> preparation,
                                 Consumer<T> action, Consumer<?> task) {
        if (!(task instanceof CompletableFuture<?> resolution)) {
            chainer.append(preparation, action);
            return;
        }
        CommandMessageTask<T> tracked = new CommandMessageTask<>(resolution, action);
        observeFailure(task, preparation);
        try {
            // FutureChain observes its own ordered link. The native immediate chainer
            // calls thenAcceptAsync directly on this preparation instead.
            chainer.append(chainer instanceof FutureChain ? preparation : new Preparation<>(preparation, tracked), tracked);
        } catch (RuntimeException | Error failure) {
            tracked.finish(failure);
            throw failure;
        }
    }

    @Override
    public void accept(T value) {
        if (this.resolution.isDone()) {
            return;
        }
        try {
            // Only the original resolver turns the native PlayerChatMessage into its API view.
            this.action.accept(value);
        } catch (RuntimeException | Error failure) {
            this.finish(failure);
            throw failure;
        }
    }

    /** Observe the previous head independently: thenCombine waits for both inputs on failure. */
    public void observePredecessor(CompletableFuture<?> predecessor) {
        predecessor.whenComplete((ignored, failure) -> {
            if (failure != null) {
                this.finish(failure);
            }
        });
    }

    /** Attach before native exceptionally recovery hides rejection or a skipped consumer. */
    public void observeLink(CompletableFuture<?> link) {
        link.whenComplete((ignored, failure) -> this.finish(failure));
    }

    private void finish(Throwable failure) {
        if (failure != null) {
            fail(this.resolution, failure);
        } else {
            // Successful delivery has already completed the same resolver future.
            // A normally completed link without delivery (closed chainer) is cancellation.
            this.resolution.cancel(false);
        }
    }

    private static void fail(CompletableFuture<?> resolution, Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) {
            failure = failure.getCause();
        }
        resolution.completeExceptionally(failure);
    }

    /** Owned by one FutureChain; no global registry or retained completed resolvers. */
    public static final class Pending {

        private final Set<CommandMessageTask<?>> tasks = new HashSet<>();
        private boolean closed;

        public void add(CommandMessageTask<?> task, boolean nativeClosed) {
            boolean cancel;
            synchronized (this) {
                cancel = this.closed || nativeClosed;
                if (!cancel && !task.resolution.isDone()) {
                    this.tasks.add(task);
                }
            }
            if (cancel) {
                task.resolution.cancel(false);
            } else {
                task.resolution.whenComplete((ignored, failure) -> {
                    synchronized (this) {
                        this.tasks.remove(task);
                    }
                });
            }
        }

        public void close() {
            ArrayList<CommandMessageTask<?>> pending;
            synchronized (this) {
                this.closed = true;
                pending = new ArrayList<>(this.tasks);
                this.tasks.clear();
            }
            // Resolver callbacks may reenter the chainer; never invoke them under the lock.
            for (CommandMessageTask<?> task : pending) {
                task.resolution.cancel(false);
            }
        }
    }

    /** Per-resolver preparation, preserving TaskChainer.immediate's executor and error logger. */
    private static final class Preparation<T> extends CompletableFuture<T> {

        private final CommandMessageTask<T> task;

        private Preparation(CompletableFuture<T> input, CommandMessageTask<T> task) {
            this.task = task;
            input.whenComplete((value, failure) -> {
                if (failure == null) {
                    this.complete(value);
                } else {
                    this.completeExceptionally(failure);
                }
            });
        }

        @Override
        public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
            CompletableFuture<Void> link = super.thenAcceptAsync(action, executor);
            this.task.observeLink(link);
            return link;
        }
    }
}
