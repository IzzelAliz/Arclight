package ca.spottedleaf.concurrentutil.util;

public final class IntegerUtil {

    public static final int HIGH_BIT_U32 = Integer.MIN_VALUE;
    public static final long HIGH_BIT_U64 = Long.MIN_VALUE;

    public static int ceilLog2(final int value) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(value - 1); // see doc of numberOfLeadingZeros
    }

    public static long ceilLog2(final long value) {
        return Long.SIZE - Long.numberOfLeadingZeros(value - 1); // see doc of numberOfLeadingZeros
    }

    public static int floorLog2(final int value) {
        // xor is optimized subtract for 2^n -1
        // note that (2^n -1) - k = (2^n -1) ^ k for k <= (2^n - 1)
        return (Integer.SIZE - 1) ^ Integer.numberOfLeadingZeros(value); // see doc of numberOfLeadingZeros
    }

    public static int floorLog2(final long value) {
        // xor is optimized subtract for 2^n -1
        // note that (2^n -1) - k = (2^n -1) ^ k for k <= (2^n - 1)
        return (Long.SIZE - 1) ^ Long.numberOfLeadingZeros(value); // see doc of numberOfLeadingZeros
    }

    public static int roundCeilLog2(final int value) {
        // optimized variant of 1 << (32 - leading(val - 1))
        // given
        // 1 << n = HIGH_BIT_32 >>> (31 - n) for n [0, 32)
        // 1 << (32 - leading(val - 1)) = HIGH_BIT_32 >>> (31 - (32 - leading(val - 1)))
        // HIGH_BIT_32 >>> (31 - (32 - leading(val - 1)))
        // HIGH_BIT_32 >>> (31 - 32 + leading(val - 1))
        // HIGH_BIT_32 >>> (-1 + leading(val - 1))
        return HIGH_BIT_U32 >>> (Integer.numberOfLeadingZeros(value - 1) - 1);
    }

    public static long roundCeilLog2(final long value) {
        // see logic documented above
        return HIGH_BIT_U64 >>> (Long.numberOfLeadingZeros(value - 1) - 1);
    }

    public static int roundFloorLog2(final int value) {
        // optimized variant of 1 << (31 - leading(val))
        // given
        // 1 << n = HIGH_BIT_32 >>> (31 - n) for n [0, 32)
        // 1 << (31 - leading(val)) = HIGH_BIT_32 >> (31 - (31 - leading(val)))
        // HIGH_BIT_32 >> (31 - (31 - leading(val)))
        // HIGH_BIT_32 >> (31 - 31 + leading(val))
        return HIGH_BIT_U32 >>> Integer.numberOfLeadingZeros(value);
    }

    public static long roundFloorLog2(final long value) {
        // see logic documented above
        return HIGH_BIT_U64 >>> Long.numberOfLeadingZeros(value);
    }

    public static boolean isPowerOfTwo(final int n) {
        // 2^n has one bit
        // note: this rets true for 0 still
        return IntegerUtil.getTrailingBit(n) == n;
    }

    public static boolean isPowerOfTwo(final long n) {
        // 2^n has one bit
        // note: this rets true for 0 still
        return IntegerUtil.getTrailingBit(n) == n;
    }

    public static int getTrailingBit(final int n) {
        return -n & n;
    }

    public static long getTrailingBit(final long n) {
        return -n & n;
    }

    public static int trailingZeros(final int n) {
        return Integer.numberOfTrailingZeros(n);
    }

    public static int trailingZeros(final long n) {
        return Long.numberOfTrailingZeros(n);
    }

    // from hacker's delight (signed division magic value)
    public static int getDivisorMultiple(final long numbers) {
        return (int)(numbers >>> 32);
    }

    // from hacker's delight (signed division magic value)
    public static int getDivisorShift(final long numbers) {
        return (int)numbers;
    }

    // copied from hacker's delight (signed division magic value)
    // http://www.hackersdelight.org/hdcodetxt/magic.c.txt
    public static long getDivisorNumbers(final int d) {
        final int ad = branchlessAbs(d);

        if (ad < 2) {
            throw new IllegalArgumentException("|number| must be in [2, 2^31 -1], not: " + d);
        }

        final int two31 = 0x80000000;
        final long mask = 0xFFFFFFFFL; // mask for enforcing unsigned behaviour

        /*
         Signed usage:
         int number;
         long magic = getDivisorNumbers(div);
         long mul = magic >>> 32;
         int sign = number >> 31;
         int result = (int)(((long)number * mul) >>> magic) - sign;
         */
        /*
         Unsigned usage: (note: fails for input > Integer.MAX_VALUE, only use when input < Integer.MAX_VALUE to avoid sign calculation)
         int number;
         long magic = getDivisorNumbers(div);
         long mul = magic >>> 32;
         int result = (int)(((long)number * mul) >>> magic);
         */

        int p = 31;

        // all these variables are UNSIGNED!
        int t = two31 + (d >>> 31);
        int anc = t - 1 - (int)((t & mask)%ad);
        int q1 = (int)((two31 & mask)/(anc & mask));
        int r1 = two31 - q1*anc;
        int q2 = (int)((two31 & mask)/(ad & mask));
        int r2 = two31 - q2*ad;
        int delta;

        do {
            p = p + 1;
            q1 = 2*q1;                        // Update q1 = 2**p/|nc|.
            r1 = 2*r1;                        // Update r1 = rem(2**p, |nc|).
            if ((r1 & mask) >= (anc & mask)) {// (Must be an unsigned comparison here)
                q1 = q1 + 1;
                r1 = r1 - anc;
            }
            q2 = 2*q2;                       // Update q2 = 2**p/|d|.
            r2 = 2*r2;                       // Update r2 = rem(2**p, |d|).
            if ((r2 & mask) >= (ad & mask)) {// (Must be an unsigned comparison here)
                q2 = q2 + 1;
                r2 = r2 - ad;
            }
            delta = ad - r2;
        } while ((q1 & mask) < (delta & mask) || (q1 == delta && r1 == 0));

        int magicNum = q2 + 1;
        if (d < 0) {
            magicNum = -magicNum;
        }
        int shift = p;
        return ((long)magicNum << 32) | shift;
    }

    public static int branchlessAbs(final int val) {
        // -n = -1 ^ n + 1
        final int mask = val >> (Integer.SIZE - 1); // -1 if < 0, 0 if >= 0
        return (mask ^ val) - mask; // if val < 0, then (0 ^ val) - 0 else (-1 ^ val) + 1
    }

    public static long branchlessAbs(final long val) {
        // -n = -1 ^ n + 1
        final long mask = val >> (Long.SIZE - 1); // -1 if < 0, 0 if >= 0
        return (mask ^ val) - mask; // if val < 0, then (0 ^ val) - 0 else (-1 ^ val) + 1
    }

    private IntegerUtil() {
        throw new RuntimeException();
    }
}