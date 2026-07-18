package ca.spottedleaf.concurrentutil.util;

public final class Validate {

    public static <T> T notNull(final T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    public static <T> T notNull(final T obj, final String msgIfNull) {
        if (obj == null) {
            throw new NullPointerException(msgIfNull);
        }
        return obj;
    }

    public static void arrayBounds(final int off, final int len, final int arrayLength, final String msgPrefix) {
        if (off < 0 || len < 0 || (arrayLength - off) < len) {
            throw new ArrayIndexOutOfBoundsException(msgPrefix + ": off: " + off + ", len: " + len + ", array length: " + arrayLength);
        }
    }

    private Validate() {
        throw new RuntimeException();
    }
}
