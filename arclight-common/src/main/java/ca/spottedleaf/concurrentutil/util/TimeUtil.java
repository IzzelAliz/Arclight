package ca.spottedleaf.concurrentutil.util;

public final class TimeUtil {

    /*
     * The comparator is not a valid comparator for every long value. To prove where it is valid, see below.
     *
     * For reflexivity, we have that x - x = 0. We then have that for any long value x that
     * compareTimes(x, x) == 0, as expected.
     *
     * For symmetry, we have that x - y = -(y - x) except for when y - x = Long.MIN_VALUE.
     * So, the difference between any times x and y must not be equal to Long.MIN_VALUE.
     *
     * As for the transitive relation, consider we have x,y such that x - y = a > 0 and z such that
     * y - z = b > 0. Then, we will have that the x - z > 0 is equivalent to a + b > 0. For long values,
     * this holds as long as a + b <= Long.MAX_VALUE.
     *
     * Also consider we have x, y such that x - y = a < 0 and z such that y - z = b < 0. Then, we will have
     * that x - z < 0 is equivalent to a + b < 0. For long values, this holds as long as a + b >= -Long.MAX_VALUE.
     *
     * Thus, the comparator is only valid for timestamps such that abs(c - d) <= Long.MAX_VALUE for all timestamps
     * c and d.
     */

    /**
     * This function is appropriate to be used as a {@link java.util.Comparator} between two timestamps, which
     * indicates whether the timestamps represented by t1, t2 that t1 is before, equal to, or after t2.
     */
    public static int compareTimes(final long t1, final long t2) {
        final long diff = t1 - t2;

        // HD, Section 2-7
        return (int) ((diff >> 63) | (-diff >>> 63));
    }

    public static long getGreatestTime(final long t1, final long t2) {
        final long diff = t1 - t2;
        return diff < 0L ? t2 : t1;
    }

    public static long getLeastTime(final long t1, final long t2) {
        final long diff = t1 - t2;
        return diff > 0L ? t2 : t1;
    }

    public static long clampTime(final long value, final long min, final long max) {
        final long diffMax = value - max;
        final long diffMin = value - min;

        if (diffMax > 0L) {
            return max;
        }
        if (diffMin < 0L) {
            return min;
        }
        return value;
    }

    private TimeUtil() {}
}
