package ca.spottedleaf.concurrentutil.util;

public final class HashUtil {

    // Copied from fastutil HashCommon

    /** 2<sup>32</sup> &middot; &phi;, &phi; = (&#x221A;5 &minus; 1)/2. */
    private static final int INT_PHI = 0x9E3779B9;
    /** The reciprocal of {@link #INT_PHI} modulo 2<sup>32</sup>. */
    private static final int INV_INT_PHI = 0x144cbc89;
    /** 2<sup>64</sup> &middot; &phi;, &phi; = (&#x221A;5 &minus; 1)/2. */
    private static final long LONG_PHI = 0x9E3779B97F4A7C15L;
    /** The reciprocal of {@link #LONG_PHI} modulo 2<sup>64</sup>. */
    private static final long INV_LONG_PHI = 0xf1de83e19937733dL;

    /** Avalanches the bits of an integer by applying the finalisation step of MurmurHash3.
     *
     * <p>This method implements the finalisation step of Austin Appleby's <a href="http://code.google.com/p/smhasher/">MurmurHash3</a>.
     * Its purpose is to avalanche the bits of the argument to within 0.25% bias.
     *
     * @param x an integer.
     * @return a hash value with good avalanching properties.
     */
    // additional note: this function is a bijection onto all integers
    public static int murmurHash3(int x) {
        x ^= x >>> 16;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        x *= 0xc2b2ae35;
        x ^= x >>> 16;
        return x;
    }


    /** Avalanches the bits of a long integer by applying the finalisation step of MurmurHash3.
     *
     * <p>This method implements the finalisation step of Austin Appleby's <a href="http://code.google.com/p/smhasher/">MurmurHash3</a>.
     * Its purpose is to avalanche the bits of the argument to within 0.25% bias.
     *
     * @param x a long integer.
     * @return a hash value with good avalanching properties.
     */
    // additional note: this function is a bijection onto all longs
    public static long murmurHash3(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    /** Quickly mixes the bits of an integer.
     *
     * <p>This method mixes the bits of the argument by multiplying by the golden ratio and
     * xorshifting the result. It is borrowed from <a href="https://github.com/leventov/Koloboke">Koloboke</a>, and
     * it has slightly worse behaviour than {@link #murmurHash3(int)} (in open-addressing hash tables the average number of probes
     * is slightly larger), but it's much faster.
     *
     * @param x an integer.
     * @return a hash value obtained by mixing the bits of {@code x}.
     * @see #invMix(int)
     */
    // additional note: this function is a bijection onto all integers
    public static int mix(final int x) {
        final int h = x * INT_PHI;
        return h ^ (h >>> 16);
    }

    /** The inverse of {@link #mix(int)}. This method is mainly useful to create unit tests.
     *
     * @param x an integer.
     * @return a value that passed through {@link #mix(int)} would give {@code x}.
     */
    // additional note: this function is a bijection onto all integers
    public static int invMix(final int x) {
        return (x ^ x >>> 16) * INV_INT_PHI;
    }

    /** Quickly mixes the bits of a long integer.
     *
     * <p>This method mixes the bits of the argument by multiplying by the golden ratio and
     * xorshifting twice the result. It is borrowed from <a href="https://github.com/leventov/Koloboke">Koloboke</a>, and
     * it has slightly worse behaviour than {@link #murmurHash3(long)} (in open-addressing hash tables the average number of probes
     * is slightly larger), but it's much faster.
     *
     * @param x a long integer.
     * @return a hash value obtained by mixing the bits of {@code x}.
     */
    // additional note: this function is a bijection onto all longs
    public static long mix(final long x) {
        long h = x * LONG_PHI;
        h ^= h >>> 32;
        return h ^ (h >>> 16);
    }

    /** The inverse of {@link #mix(long)}. This method is mainly useful to create unit tests.
     *
     * @param x a long integer.
     * @return a value that passed through {@link #mix(long)} would give {@code x}.
     */
    // additional note: this function is a bijection onto all longs
    public static long invMix(long x) {
        x ^= x >>> 32;
        x ^= x >>> 16;
        return (x ^ x >>> 32) * INV_LONG_PHI;
    }


    private HashUtil() {}
}
