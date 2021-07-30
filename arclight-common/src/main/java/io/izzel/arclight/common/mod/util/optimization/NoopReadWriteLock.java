package io.izzel.arclight.common.mod.util.optimization;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class NoopReadWriteLock implements ReadWriteLock {

    private static final NoopReadWriteLock INSTANCE = new NoopReadWriteLock();
    private static final NoopLock LOCK = new NoopLock();

    @NotNull
    @Override
    public Lock readLock() {
        return LOCK;
    }

    @NotNull
    @Override
    public Lock writeLock() {
        return LOCK;
    }

    public static ReadWriteLock instance() {
        return INSTANCE;
    }

    private static class NoopLock implements Lock {

        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
        }

        @NotNull
        @Override
        public Condition newCondition() {
            throw new AssertionError();
        }
    }
}
