package io.izzel.arclight.common.mod.util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Reuses the fertilization helper's XP roll without advancing the native RNG twice. */
public final class FertilizeEggRandom implements RandomSource {

    private final RandomSource delegate;
    private final int experience;

    public FertilizeEggRandom(RandomSource delegate, int experience) {
        this.delegate = delegate;
        this.experience = experience;
    }

    @Override
    public int nextInt(int bound) {
        if (bound == 7) {
            // Turtle's retained caller adds one after this invocation.
            return experience - 1;
        }
        return delegate.nextInt(bound);
    }

    @Override
    public RandomSource fork() {
        return delegate.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return delegate.forkPositional();
    }

    @Override
    public void setSeed(long seed) {
        delegate.setSeed(seed);
    }

    @Override
    public int nextInt() {
        return delegate.nextInt();
    }

    @Override
    public long nextLong() {
        return delegate.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return delegate.nextBoolean();
    }

    @Override
    public float nextFloat() {
        return delegate.nextFloat();
    }

    @Override
    public double nextDouble() {
        return delegate.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return delegate.nextGaussian();
    }

    @Override
    public int nextIntBetweenInclusive(int min, int max) {
        return delegate.nextIntBetweenInclusive(min, max);
    }

    @Override
    public int nextInt(int min, int max) {
        return delegate.nextInt(min, max);
    }

    @Override
    public double triangle(double mean, double deviation) {
        return delegate.triangle(mean, deviation);
    }

    @Override
    public float triangle(float mean, float deviation) {
        return delegate.triangle(mean, deviation);
    }

    @Override
    public void consumeCount(int count) {
        delegate.consumeCount(count);
    }
}
