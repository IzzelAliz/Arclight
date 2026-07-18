package ca.spottedleaf.concurrentutil.util;

import java.lang.invoke.VarHandle;

public final class ArrayUtil {

    public static final VarHandle BOOLEAN_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(boolean[].class);

    public static final VarHandle BYTE_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(byte[].class);

    public static final VarHandle SHORT_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(short[].class);

    public static final VarHandle INT_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(int[].class);

    public static final VarHandle LONG_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(long[].class);

    public static final VarHandle OBJECT_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(Object[].class);

    private ArrayUtil() {
        throw new RuntimeException();
    }

    /* byte array */

    public static byte getPlain(final byte[] array, final int index) {
        return (byte)BYTE_ARRAY_HANDLE.get(array, index);
    }

    public static byte getOpaque(final byte[] array, final int index) {
        return (byte)BYTE_ARRAY_HANDLE.getOpaque(array, index);
    }

    public static byte getAcquire(final byte[] array, final int index) {
        return (byte)BYTE_ARRAY_HANDLE.getAcquire(array, index);
    }

    public static byte getVolatile(final byte[] array, final int index) {
        return (byte)BYTE_ARRAY_HANDLE.getVolatile(array, index);
    }

    public static void setPlain(final byte[] array, final int index, final byte value) {
        BYTE_ARRAY_HANDLE.set(array, index, value);
    }

    public static void setOpaque(final byte[] array, final int index, final byte value) {
        BYTE_ARRAY_HANDLE.setOpaque(array, index, value);
    }

    public static void setRelease(final byte[] array, final int index, final byte value) {
        BYTE_ARRAY_HANDLE.setRelease(array, index, value);
    }

    public static void setVolatile(final byte[] array, final int index, final byte value) {
        BYTE_ARRAY_HANDLE.setVolatile(array, index, value);
    }

    public static void setVolatileContended(final byte[] array, final int index, final byte param) {
        int failures = 0;

        for (byte curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return;
            }
        }
    }

    public static byte compareAndExchangeVolatile(final byte[] array, final int index, final byte expect, final byte update) {
        return (byte)BYTE_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static byte getAndAddVolatile(final byte[] array, final int index, final byte param) {
        return (byte)BYTE_ARRAY_HANDLE.getAndAdd(array, index, param);
    }

    public static byte getAndAndVolatile(final byte[] array, final int index, final byte param) {
        return (byte)BYTE_ARRAY_HANDLE.getAndBitwiseAnd(array, index, param);
    }

    public static byte getAndOrVolatile(final byte[] array, final int index, final byte param) {
        return (byte)BYTE_ARRAY_HANDLE.getAndBitwiseOr(array, index, param);
    }

    public static byte getAndXorVolatile(final byte[] array, final int index, final byte param) {
        return (byte)BYTE_ARRAY_HANDLE.getAndBitwiseXor(array, index, param);
    }

    public static byte getAndSetVolatile(final byte[] array, final int index, final byte param) {
        return (byte)BYTE_ARRAY_HANDLE.getAndSet(array, index, param);
    }

    public static byte compareAndExchangeVolatileContended(final byte[] array, final int index, final byte expect, final byte update) {
        return (byte)BYTE_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static byte getAndAddVolatileContended(final byte[] array, final int index, final byte param) {
        int failures = 0;

        for (byte curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (byte) (curr + param)))) {
                return curr;
            }
        }
    }

    public static byte getAndAndVolatileContended(final byte[] array, final int index, final byte param) {
        int failures = 0;

        for (byte curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (byte) (curr & param)))) {
                return curr;
            }
        }
    }

    public static byte getAndOrVolatileContended(final byte[] array, final int index, final byte param) {
        int failures = 0;

        for (byte curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (byte) (curr | param)))) {
                return curr;
            }
        }
    }

    public static byte getAndXorVolatileContended(final byte[] array, final int index, final byte param) {
        int failures = 0;

        for (byte curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (byte) (curr ^ param)))) {
                return curr;
            }
        }
    }

    public static byte getAndSetVolatileContended(final byte[] array, final int index, final byte param) {
        int failures = 0;

        for (byte curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return curr;
            }
        }
    }

    /* short array */

    public static short getPlain(final short[] array, final int index) {
        return (short)SHORT_ARRAY_HANDLE.get(array, index);
    }

    public static short getOpaque(final short[] array, final int index) {
        return (short)SHORT_ARRAY_HANDLE.getOpaque(array, index);
    }

    public static short getAcquire(final short[] array, final int index) {
        return (short)SHORT_ARRAY_HANDLE.getAcquire(array, index);
    }

    public static short getVolatile(final short[] array, final int index) {
        return (short)SHORT_ARRAY_HANDLE.getVolatile(array, index);
    }

    public static void setPlain(final short[] array, final int index, final short value) {
        SHORT_ARRAY_HANDLE.set(array, index, value);
    }

    public static void setOpaque(final short[] array, final int index, final short value) {
        SHORT_ARRAY_HANDLE.setOpaque(array, index, value);
    }

    public static void setRelease(final short[] array, final int index, final short value) {
        SHORT_ARRAY_HANDLE.setRelease(array, index, value);
    }

    public static void setVolatile(final short[] array, final int index, final short value) {
        SHORT_ARRAY_HANDLE.setVolatile(array, index, value);
    }

    public static void setVolatileContended(final short[] array, final int index, final short param) {
        int failures = 0;

        for (short curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return;
            }
        }
    }

    public static short compareAndExchangeVolatile(final short[] array, final int index, final short expect, final short update) {
        return (short)SHORT_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static short getAndAddVolatile(final short[] array, final int index, final short param) {
        return (short)SHORT_ARRAY_HANDLE.getAndAdd(array, index, param);
    }

    public static short getAndAndVolatile(final short[] array, final int index, final short param) {
        return (short)SHORT_ARRAY_HANDLE.getAndBitwiseAnd(array, index, param);
    }

    public static short getAndOrVolatile(final short[] array, final int index, final short param) {
        return (short)SHORT_ARRAY_HANDLE.getAndBitwiseOr(array, index, param);
    }

    public static short getAndXorVolatile(final short[] array, final int index, final short param) {
        return (short)SHORT_ARRAY_HANDLE.getAndBitwiseXor(array, index, param);
    }

    public static short getAndSetVolatile(final short[] array, final int index, final short param) {
        return (short)SHORT_ARRAY_HANDLE.getAndSet(array, index, param);
    }

    public static short compareAndExchangeVolatileContended(final short[] array, final int index, final short expect, final short update) {
        return (short)SHORT_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static short getAndAddVolatileContended(final short[] array, final int index, final short param) {
        int failures = 0;

        for (short curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (short) (curr + param)))) {
                return curr;
            }
        }
    }

    public static short getAndAndVolatileContended(final short[] array, final int index, final short param) {
        int failures = 0;

        for (short curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (short) (curr & param)))) {
                return curr;
            }
        }
    }

    public static short getAndOrVolatileContended(final short[] array, final int index, final short param) {
        int failures = 0;

        for (short curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (short) (curr | param)))) {
                return curr;
            }
        }
    }

    public static short getAndXorVolatileContended(final short[] array, final int index, final short param) {
        int failures = 0;

        for (short curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (short) (curr ^ param)))) {
                return curr;
            }
        }
    }

    public static short getAndSetVolatileContended(final short[] array, final int index, final short param) {
        int failures = 0;

        for (short curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return curr;
            }
        }
    }

    /* int array */

    public static int getPlain(final int[] array, final int index) {
        return (int)INT_ARRAY_HANDLE.get(array, index);
    }

    public static int getOpaque(final int[] array, final int index) {
        return (int)INT_ARRAY_HANDLE.getOpaque(array, index);
    }

    public static int getAcquire(final int[] array, final int index) {
        return (int)INT_ARRAY_HANDLE.getAcquire(array, index);
    }

    public static int getVolatile(final int[] array, final int index) {
        return (int)INT_ARRAY_HANDLE.getVolatile(array, index);
    }

    public static void setPlain(final int[] array, final int index, final int value) {
        INT_ARRAY_HANDLE.set(array, index, value);
    }

    public static void setOpaque(final int[] array, final int index, final int value) {
        INT_ARRAY_HANDLE.setOpaque(array, index, value);
    }

    public static void setRelease(final int[] array, final int index, final int value) {
        INT_ARRAY_HANDLE.setRelease(array, index, value);
    }

    public static void setVolatile(final int[] array, final int index, final int value) {
        INT_ARRAY_HANDLE.setVolatile(array, index, value);
    }

    public static void setVolatileContended(final int[] array, final int index, final int param) {
        int failures = 0;

        for (int curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return;
            }
        }
    }

    public static int compareAndExchangeVolatile(final int[] array, final int index, final int expect, final int update) {
        return (int)INT_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static int getAndAddVolatile(final int[] array, final int index, final int param) {
        return (int)INT_ARRAY_HANDLE.getAndAdd(array, index, param);
    }

    public static int getAndAndVolatile(final int[] array, final int index, final int param) {
        return (int)INT_ARRAY_HANDLE.getAndBitwiseAnd(array, index, param);
    }

    public static int getAndOrVolatile(final int[] array, final int index, final int param) {
        return (int)INT_ARRAY_HANDLE.getAndBitwiseOr(array, index, param);
    }

    public static int getAndXorVolatile(final int[] array, final int index, final int param) {
        return (int)INT_ARRAY_HANDLE.getAndBitwiseXor(array, index, param);
    }

    public static int getAndSetVolatile(final int[] array, final int index, final int param) {
        return (int)INT_ARRAY_HANDLE.getAndSet(array, index, param);
    }

    public static int compareAndExchangeVolatileContended(final int[] array, final int index, final int expect, final int update) {
        return (int)INT_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static int getAndAddVolatileContended(final int[] array, final int index, final int param) {
        int failures = 0;

        for (int curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (int) (curr + param)))) {
                return curr;
            }
        }
    }

    public static int getAndAndVolatileContended(final int[] array, final int index, final int param) {
        int failures = 0;

        for (int curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (int) (curr & param)))) {
                return curr;
            }
        }
    }

    public static int getAndOrVolatileContended(final int[] array, final int index, final int param) {
        int failures = 0;

        for (int curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (int) (curr | param)))) {
                return curr;
            }
        }
    }

    public static int getAndXorVolatileContended(final int[] array, final int index, final int param) {
        int failures = 0;

        for (int curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (int) (curr ^ param)))) {
                return curr;
            }
        }
    }

    public static int getAndSetVolatileContended(final int[] array, final int index, final int param) {
        int failures = 0;

        for (int curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return curr;
            }
        }
    }

    /* long array */

    public static long getPlain(final long[] array, final int index) {
        return (long)LONG_ARRAY_HANDLE.get(array, index);
    }

    public static long getOpaque(final long[] array, final int index) {
        return (long)LONG_ARRAY_HANDLE.getOpaque(array, index);
    }

    public static long getAcquire(final long[] array, final int index) {
        return (long)LONG_ARRAY_HANDLE.getAcquire(array, index);
    }

    public static long getVolatile(final long[] array, final int index) {
        return (long)LONG_ARRAY_HANDLE.getVolatile(array, index);
    }

    public static void setPlain(final long[] array, final int index, final long value) {
        LONG_ARRAY_HANDLE.set(array, index, value);
    }

    public static void setOpaque(final long[] array, final int index, final long value) {
        LONG_ARRAY_HANDLE.setOpaque(array, index, value);
    }

    public static void setRelease(final long[] array, final int index, final long value) {
        LONG_ARRAY_HANDLE.setRelease(array, index, value);
    }

    public static void setVolatile(final long[] array, final int index, final long value) {
        LONG_ARRAY_HANDLE.setVolatile(array, index, value);
    }

    public static void setVolatileContended(final long[] array, final int index, final long param) {
        int failures = 0;

        for (long curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return;
            }
        }
    }

    public static long compareAndExchangeVolatile(final long[] array, final int index, final long expect, final long update) {
        return (long)LONG_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static long getAndAddVolatile(final long[] array, final int index, final long param) {
        return (long)LONG_ARRAY_HANDLE.getAndAdd(array, index, param);
    }

    public static long getAndAndVolatile(final long[] array, final int index, final long param) {
        return (long)LONG_ARRAY_HANDLE.getAndBitwiseAnd(array, index, param);
    }

    public static long getAndOrVolatile(final long[] array, final int index, final long param) {
        return (long)LONG_ARRAY_HANDLE.getAndBitwiseOr(array, index, param);
    }

    public static long getAndXorVolatile(final long[] array, final int index, final long param) {
        return (long)LONG_ARRAY_HANDLE.getAndBitwiseXor(array, index, param);
    }

    public static long getAndSetVolatile(final long[] array, final int index, final long param) {
        return (long)LONG_ARRAY_HANDLE.getAndSet(array, index, param);
    }

    public static long compareAndExchangeVolatileContended(final long[] array, final int index, final long expect, final long update) {
        return (long)LONG_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static long getAndAddVolatileContended(final long[] array, final int index, final long param) {
        int failures = 0;

        for (long curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (long) (curr + param)))) {
                return curr;
            }
        }
    }

    public static long getAndAndVolatileContended(final long[] array, final int index, final long param) {
        int failures = 0;

        for (long curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (long) (curr & param)))) {
                return curr;
            }
        }
    }

    public static long getAndOrVolatileContended(final long[] array, final int index, final long param) {
        int failures = 0;

        for (long curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (long) (curr | param)))) {
                return curr;
            }
        }
    }

    public static long getAndXorVolatileContended(final long[] array, final int index, final long param) {
        int failures = 0;

        for (long curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (long) (curr ^ param)))) {
                return curr;
            }
        }
    }

    public static long getAndSetVolatileContended(final long[] array, final int index, final long param) {
        int failures = 0;

        for (long curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return curr;
            }
        }
    }

    /* boolean array */

    public static boolean getPlain(final boolean[] array, final int index) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.get(array, index);
    }

    public static boolean getOpaque(final boolean[] array, final int index) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.getOpaque(array, index);
    }

    public static boolean getAcquire(final boolean[] array, final int index) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.getAcquire(array, index);
    }

    public static boolean getVolatile(final boolean[] array, final int index) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.getVolatile(array, index);
    }

    public static void setPlain(final boolean[] array, final int index, final boolean value) {
        BOOLEAN_ARRAY_HANDLE.set(array, index, value);
    }

    public static void setOpaque(final boolean[] array, final int index, final boolean value) {
        BOOLEAN_ARRAY_HANDLE.setOpaque(array, index, value);
    }

    public static void setRelease(final boolean[] array, final int index, final boolean value) {
        BOOLEAN_ARRAY_HANDLE.setRelease(array, index, value);
    }

    public static void setVolatile(final boolean[] array, final int index, final boolean value) {
        BOOLEAN_ARRAY_HANDLE.setVolatile(array, index, value);
    }

    public static void setVolatileContended(final boolean[] array, final int index, final boolean param) {
        int failures = 0;

        for (boolean curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return;
            }
        }
    }

    public static boolean compareAndExchangeVolatile(final boolean[] array, final int index, final boolean expect, final boolean update) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static boolean getAndOrVolatile(final boolean[] array, final int index, final boolean param) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.getAndBitwiseOr(array, index, param);
    }

    public static boolean getAndXorVolatile(final boolean[] array, final int index, final boolean param) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.getAndBitwiseXor(array, index, param);
    }

    public static boolean getAndSetVolatile(final boolean[] array, final int index, final boolean param) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.getAndSet(array, index, param);
    }

    public static boolean compareAndExchangeVolatileContended(final boolean[] array, final int index, final boolean expect, final boolean update) {
        return (boolean)BOOLEAN_ARRAY_HANDLE.compareAndExchange(array, index, expect, update);
    }

    public static boolean getAndAndVolatileContended(final boolean[] array, final int index, final boolean param) {
        int failures = 0;

        for (boolean curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (boolean) (curr & param)))) {
                return curr;
            }
        }
    }

    public static boolean getAndOrVolatileContended(final boolean[] array, final int index, final boolean param) {
        int failures = 0;

        for (boolean curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (boolean) (curr | param)))) {
                return curr;
            }
        }
    }

    public static boolean getAndXorVolatileContended(final boolean[] array, final int index, final boolean param) {
        int failures = 0;

        for (boolean curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, (boolean) (curr ^ param)))) {
                return curr;
            }
        }
    }

    public static boolean getAndSetVolatileContended(final boolean[] array, final int index, final boolean param) {
        int failures = 0;

        for (boolean curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return curr;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getPlain(final T[] array, final int index) {
        final Object ret = OBJECT_ARRAY_HANDLE.get((Object[])array, index);
        return (T)ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOpaque(final T[] array, final int index) {
        final Object ret = OBJECT_ARRAY_HANDLE.getOpaque((Object[])array, index);
        return (T)ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAcquire(final T[] array, final int index) {
        final Object ret = OBJECT_ARRAY_HANDLE.getAcquire((Object[])array, index);
        return (T)ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getVolatile(final T[] array, final int index) {
        final Object ret = OBJECT_ARRAY_HANDLE.getVolatile((Object[])array, index);
        return (T)ret;
    }

    public static <T> void setPlain(final T[] array, final int index, final T value) {
        OBJECT_ARRAY_HANDLE.set((Object[])array, index, (Object)value);
    }

    public static <T> void setOpaque(final T[] array, final int index, final T value) {
        OBJECT_ARRAY_HANDLE.setOpaque((Object[])array, index, (Object)value);
    }

    public static <T> void setRelease(final T[] array, final int index, final T value) {
        OBJECT_ARRAY_HANDLE.setRelease((Object[])array, index, (Object)value);
    }

    public static <T> void setVolatile(final T[] array, final int index, final T value) {
        OBJECT_ARRAY_HANDLE.setVolatile((Object[])array, index, (Object)value);
    }

    public static <T> void setVolatileContended(final T[] array, final int index, final T param) {
        int failures = 0;

        for (T curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T compareAndExchangeVolatile(final T[] array, final int index, final T expect, final T update) {
        final Object ret = OBJECT_ARRAY_HANDLE.compareAndExchange((Object[])array, index, (Object)expect, (Object)update);
        return (T)ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAndSetVolatile(final T[] array, final int index, final T param) {
        final Object ret = BYTE_ARRAY_HANDLE.getAndSet((Object[])array, index, (Object)param);
        return (T)ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T compareAndExchangeVolatileContended(final T[] array, final int index, final T expect, final T update) {
        final Object ret = OBJECT_ARRAY_HANDLE.compareAndExchange((Object[])array, index, (Object)expect, (Object)update);
        return (T)ret;
    }

    public static <T> T getAndSetVolatileContended(final T[] array, final int index, final T param) {
        int failures = 0;

        for (T curr = getVolatile(array, index);;++failures) {
            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = compareAndExchangeVolatileContended(array, index, curr, param))) {
                return curr;
            }
        }
    }
}
