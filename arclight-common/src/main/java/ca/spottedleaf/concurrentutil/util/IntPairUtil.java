package ca.spottedleaf.concurrentutil.util;

public final class IntPairUtil {

    /**
     * Packs the specified integers into one long value.
     */
    public static long key(final int left, final int right) {
        return ((long)right << 32) | (left & 0xFFFFFFFFL);
    }

    /**
     * Retrieves the left packed integer from the key
     */
    public static int left(final long key) {
        return (int)key;
    }

    /**
     * Retrieves the right packed integer from the key
     */
    public static int right(final long key) {
        return (int)(key >>> 32);
    }

    public static String toString(final long key) {
        return "{left:" + left(key) + ", right:" + right(key) + "}";
    }

    public static String toString(final long[] array, final int from, final int to) {
        final StringBuilder ret = new StringBuilder();
        ret.append("[");

        for (int i = from; i < to; ++i) {
            if (i != from) {
                ret.append(", ");
            }
            ret.append(toString(array[i]));
        }

        ret.append("]");
        return ret.toString();
    }

    private IntPairUtil() {}
}
