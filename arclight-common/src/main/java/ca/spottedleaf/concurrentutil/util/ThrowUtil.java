package ca.spottedleaf.concurrentutil.util;

public final class ThrowUtil {

    private ThrowUtil() {}

    public static <T extends Throwable> void throwUnchecked(final Throwable thr) throws T {
        throw (T)thr;
    }

}
