package ca.spottedleaf.concurrentutil.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

public final class ConcurrentUtil {

    public static String genericToString(final Object object) {
        return object == null ? "null" : object.getClass().getName() + ":" + object.hashCode() + ":" + object.toString();
    }

    public static void rethrow(Throwable exception) {
        rethrow0(exception);
    }

    private static <T extends Throwable> void rethrow0(Throwable thr) throws T {
        throw (T)thr;
    }

    public static VarHandle getVarHandle(final Class<?> lookIn, final String fieldName, final Class<?> fieldType) {
        try {
            return MethodHandles.privateLookupIn(lookIn, MethodHandles.lookup()).findVarHandle(lookIn, fieldName, fieldType);
        } catch (final Exception ex) {
            throw new RuntimeException(ex); // unreachable
        }
    }

    public static VarHandle getStaticVarHandle(final Class<?> lookIn, final String fieldName, final Class<?> fieldType) {
        try {
            return MethodHandles.privateLookupIn(lookIn, MethodHandles.lookup()).findStaticVarHandle(lookIn, fieldName, fieldType);
        } catch (final Exception ex) {
            throw new RuntimeException(ex); // unreachable
        }
    }

    /**
     * Non-exponential backoff algorithm to use in lightly contended areas.
     * @see ConcurrentUtil#exponentiallyBackoffSimple(long)
     * @see ConcurrentUtil#exponentiallyBackoffComplex(long)
     */
    public static void backoff() {
        Thread.onSpinWait();
    }

    /**
     * Backoff algorithm to use for a short held lock (i.e compareAndExchange operation). Generally this should not be
     * used when a thread can block another thread. Instead, use {@link ConcurrentUtil#exponentiallyBackoffComplex(long)}.
     * @param counter The current counter.
     * @return The counter plus 1.
     * @see ConcurrentUtil#backoff()
     * @see ConcurrentUtil#exponentiallyBackoffComplex(long)
     */
    public static long exponentiallyBackoffSimple(final long counter) {
        for (long i = 0; i < counter; ++i) {
            backoff();
        }
        return counter + 1L;
    }

    /**
     * Backoff algorithm to use for a lock that can block other threads (i.e if another thread contending with this thread
     * can be thrown off the scheduler). This lock should not be used for simple locks such as compareAndExchange.
     * @param counter The current counter.
     * @return The next (if any) step in the backoff logic.
     * @see ConcurrentUtil#backoff()
     * @see ConcurrentUtil#exponentiallyBackoffSimple(long)
     */
    public static long exponentiallyBackoffComplex(final long counter) {
        // TODO experimentally determine counters
        if (counter < 100L) {
            return exponentiallyBackoffSimple(counter);
        }
        if (counter < 1_200L) {
            Thread.yield();
            LockSupport.parkNanos(1_000L);
            return counter + 1L;
        }
        // scale 0.1ms (100us) per failure
        Thread.yield();
        LockSupport.parkNanos(100_000L * counter);
        return counter + 1;
    }

    /**
     * Simple exponential backoff that will linearly increase the time per failure, according to the scale.
     * @param counter The current failure counter.
     * @param scale Time per failure, in ns.
     * @param max The maximum time to wait for, in ns.
     * @return The next counter.
     */
    public static long linearLongBackoff(long counter, final long scale, long max) {
        counter = Math.min(Long.MAX_VALUE, counter + 1); // prevent overflow
        max = Math.max(0, max);

        if (scale <= 0L) {
            return counter;
        }

        long time = scale * counter;

        if (time > max || time / scale != counter) {
            time = max;
        }

        boolean interrupted = Thread.interrupted();
        if (time > 1_000_000L) { // 1ms
            Thread.yield();
        }
        LockSupport.parkNanos(time);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return counter;
    }

    /**
     * Simple exponential backoff that will linearly increase the time per failure, according to the scale.
     * @param counter The current failure counter.
     * @param scale Time per failure, in ns.
     * @param max The maximum time to wait for, in ns.
     * @param deadline The deadline in ns. Deadline time source: {@link System#nanoTime()}.
     * @return The next counter.
     */
    public static long linearLongBackoffDeadline(long counter, final long scale, long max, long deadline) {
        counter = Math.min(Long.MAX_VALUE, counter + 1); // prevent overflow
        max = Math.max(0, max);

        if (scale <= 0L) {
            return counter;
        }

        long time = scale * counter;

        // check overflow
        if (time / scale != counter) {
            // overflew
            --counter;
            time = max;
        } else if (time > max) {
            time = max;
        }

        final long currTime = System.nanoTime();
        final long diff = deadline - currTime;
        if (diff <= 0) {
            return counter;
        }
        if (diff <= 1_500_000L) { // 1.5ms
            time = 100_000L; // 100us
        } else if (time > 1_000_000L) { // 1ms
            Thread.yield();
        }

        boolean interrupted = Thread.interrupted();
        LockSupport.parkNanos(time);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return counter;
    }

    public static VarHandle getArrayHandle(final Class<?> type) {
        return MethodHandles.arrayElementVarHandle(type);
    }
}
