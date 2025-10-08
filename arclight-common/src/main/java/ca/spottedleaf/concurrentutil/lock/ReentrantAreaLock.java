package ca.spottedleaf.concurrentutil.lock;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;
import ca.spottedleaf.concurrentutil.map.ConcurrentLong2ReferenceChainedHashTable;
import ca.spottedleaf.concurrentutil.util.IntPairUtil;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

public final class ReentrantAreaLock {

    public final int coordinateShift;

    // aggressive load factor to reduce contention
    private final ConcurrentLong2ReferenceChainedHashTable<Node> nodes = ConcurrentLong2ReferenceChainedHashTable.createWithCapacity(128, 0.2f);

    public ReentrantAreaLock(final int coordinateShift) {
        this.coordinateShift = coordinateShift;
    }

    public boolean isHeldByCurrentThread(final int x, final int z) {
        final Thread currThread = Thread.currentThread();
        final int shift = this.coordinateShift;
        final int sectionX = x >> shift;
        final int sectionZ = z >> shift;

        final long coordinate = IntPairUtil.key(sectionX, sectionZ);
        final Node node = this.nodes.get(coordinate);

        return node != null && node.thread == currThread;
    }

    public boolean isHeldByCurrentThread(final int centerX, final int centerZ, final int radius) {
        return this.isHeldByCurrentThread(centerX - radius, centerZ - radius, centerX + radius, centerZ + radius);
    }

    public boolean isHeldByCurrentThread(final int fromX, final int fromZ, final int toX, final int toZ) {
        if (fromX > toX || fromZ > toZ) {
            throw new IllegalArgumentException();
        }

        final Thread currThread = Thread.currentThread();
        final int shift = this.coordinateShift;
        final int fromSectionX = fromX >> shift;
        final int fromSectionZ = fromZ >> shift;
        final int toSectionX = toX >> shift;
        final int toSectionZ = toZ >> shift;

        for (int currZ = fromSectionZ; currZ <= toSectionZ; ++currZ) {
            for (int currX = fromSectionX; currX <= toSectionX; ++currX) {
                final long coordinate = IntPairUtil.key(currX, currZ);

                final Node node = this.nodes.get(coordinate);

                if (node == null || node.thread != currThread) {
                    return false;
                }
            }
        }

        return true;
    }

    public Node tryLock(final int x, final int z) {
        return this.tryLock(x, z, x, z);
    }

    public Node tryLock(final int centerX, final int centerZ, final int radius) {
        return this.tryLock(centerX - radius, centerZ - radius, centerX + radius, centerZ + radius);
    }

    public Node tryLock(final int fromX, final int fromZ, final int toX, final int toZ) {
        if (fromX > toX || fromZ > toZ) {
            throw new IllegalArgumentException();
        }

        final Thread currThread = Thread.currentThread();
        final int shift = this.coordinateShift;
        final int fromSectionX = fromX >> shift;
        final int fromSectionZ = fromZ >> shift;
        final int toSectionX = toX >> shift;
        final int toSectionZ = toZ >> shift;

        final long[] areaAffected = new long[(toSectionX - fromSectionX + 1) * (toSectionZ - fromSectionZ + 1)];
        int areaAffectedLen = 0;

        final Node ret = new Node(this, areaAffected, currThread);

        boolean failed = false;

        // try to fast acquire area
        for (int currZ = fromSectionZ; currZ <= toSectionZ; ++currZ) {
            for (int currX = fromSectionX; currX <= toSectionX; ++currX) {
                final long coordinate = IntPairUtil.key(currX, currZ);

                final Node prev = this.nodes.putIfAbsent(coordinate, ret);

                if (prev == null) {
                    areaAffected[areaAffectedLen++] = coordinate;
                    continue;
                }

                if (prev.thread != currThread) {
                    failed = true;
                    break;
                }
            }
        }

        if (!failed) {
            return ret;
        }

        // failed, undo logic
        if (areaAffectedLen != 0) {
            for (int i = 0; i < areaAffectedLen; ++i) {
                final long key = areaAffected[i];

                if (this.nodes.remove(key) != ret) {
                    throw new IllegalStateException();
                }
            }

            areaAffectedLen = 0;

            // since we inserted, we need to drain waiters
            Thread unpark;
            while ((unpark = ret.pollOrBlockAdds()) != null) {
                LockSupport.unpark(unpark);
            }
        }

        return null;
    }

    public Node lock(final int x, final int z) {
        final Thread currThread = Thread.currentThread();
        final int shift = this.coordinateShift;
        final int sectionX = x >> shift;
        final int sectionZ = z >> shift;

        final long coordinate = IntPairUtil.key(sectionX, sectionZ);
        final long[] areaAffected = new long[1];
        areaAffected[0] = coordinate;

        final Node ret = new Node(this, areaAffected, currThread);

        for (long failures = 0L;;) {
            final Node park;

            // try to fast acquire area
            {
                final Node prev = this.nodes.putIfAbsent(coordinate, ret);

                if (prev == null) {
                    ret.areaAffectedLen = 1;
                    return ret;
                } else if (prev.thread != currThread) {
                    park = prev;
                } else {
                    // only one node we would want to acquire, and it's owned by this thread already
                    // areaAffectedLen = 0 already
                    return ret;
                }
            }

            ++failures;

            if (failures > 128L && park.add(currThread)) {
                LockSupport.park();
            } else {
                // high contention, spin wait
                if (failures < 128L) {
                    for (long i = 0; i < failures; ++i) {
                        Thread.onSpinWait();
                    }
                    failures = failures << 1;
                } else if (failures < 1_200L) {
                    LockSupport.parkNanos(1_000L);
                    failures = failures + 1L;
                } else { // scale 0.1ms (100us) per failure
                    Thread.yield();
                    LockSupport.parkNanos(100_000L * failures);
                    failures = failures + 1L;
                }
            }
        }
    }

    public Node lock(final int centerX, final int centerZ, final int radius) {
        return this.lock(centerX - radius, centerZ - radius, centerX + radius, centerZ + radius);
    }

    public Node lock(final int fromX, final int fromZ, final int toX, final int toZ) {
        if (fromX > toX || fromZ > toZ) {
            throw new IllegalArgumentException();
        }

        final Thread currThread = Thread.currentThread();
        final int shift = this.coordinateShift;
        final int fromSectionX = fromX >> shift;
        final int fromSectionZ = fromZ >> shift;
        final int toSectionX = toX >> shift;
        final int toSectionZ = toZ >> shift;

        if (((fromSectionX ^ toSectionX) | (fromSectionZ ^ toSectionZ)) == 0) {
            return this.lock(fromX, fromZ);
        }

        final long[] areaAffected = new long[(toSectionX - fromSectionX + 1) * (toSectionZ - fromSectionZ + 1)];
        int areaAffectedLen = 0;

        final Node ret = new Node(this, areaAffected, currThread);

        for (long failures = 0L;;) {
            Node park = null;
            boolean addedToArea = false;
            boolean alreadyOwned = false;
            boolean allOwned = true;

            // try to fast acquire area
            for (int currZ = fromSectionZ; currZ <= toSectionZ; ++currZ) {
                for (int currX = fromSectionX; currX <= toSectionX; ++currX) {
                    final long coordinate = IntPairUtil.key(currX, currZ);

                    final Node prev = this.nodes.putIfAbsent(coordinate, ret);

                    if (prev == null) {
                        addedToArea = true;
                        allOwned = false;
                        areaAffected[areaAffectedLen++] = coordinate;
                        continue;
                    }

                    if (prev.thread != currThread) {
                        park = prev;
                        alreadyOwned = true;
                        break;
                    }
                }
            }

            // check for failure
            if ((park != null && addedToArea) || (park == null && alreadyOwned && !allOwned)) {
                // failure to acquire: added and we need to block, or improper lock usage
                for (int i = 0; i < areaAffectedLen; ++i) {
                    final long key = areaAffected[i];

                    if (this.nodes.remove(key) != ret) {
                        throw new IllegalStateException();
                    }
                }

                areaAffectedLen = 0;

                // since we inserted, we need to drain waiters
                Thread unpark;
                while ((unpark = ret.pollOrBlockAdds()) != null) {
                    LockSupport.unpark(unpark);
                }
            }

            if (park == null) {
                if (alreadyOwned && !allOwned) {
                    throw new IllegalStateException("Improper lock usage: Should never acquire intersecting areas");
                }
                ret.areaAffectedLen = areaAffectedLen;
                return ret;
            }

            // failed

            ++failures;

            if (failures > 128L && park.add(currThread)) {
                LockSupport.park(park);
            } else {
                // high contention, spin wait
                if (failures < 128L) {
                    for (long i = 0; i < failures; ++i) {
                        Thread.onSpinWait();
                    }
                    failures = failures << 1;
                } else if (failures < 1_200L) {
                    LockSupport.parkNanos(1_000L);
                    failures = failures + 1L;
                } else { // scale 0.1ms (100us) per failure
                    Thread.yield();
                    LockSupport.parkNanos(100_000L * failures);
                    failures = failures + 1L;
                }
            }

            if (addedToArea) {
                // try again, so we need to allow adds so that other threads can properly block on us
                ret.allowAdds();
            }
        }
    }

    public void unlock(final Node node) {
        if (node.lock != this) {
            throw new IllegalStateException("Unlock target lock mismatch");
        }

        final long[] areaAffected = node.areaAffected;
        final int areaAffectedLen = node.areaAffectedLen;

        if (areaAffectedLen == 0) {
            // here we are not in the node map, and so do not need to remove from the node map or unblock any waiters
            return;
        }

        Objects.checkFromToIndex(0, areaAffectedLen, areaAffected.length);

        // remove from node map; allowing other threads to lock
        for (int i = 0; i < areaAffectedLen; ++i) {
            final long coordinate = areaAffected[i];
            if (this.nodes.remove(coordinate, node) != node) {
                throw new IllegalStateException();
            }
        }

        Thread unpark;
        while ((unpark = node.pollOrBlockAdds()) != null) {
            LockSupport.unpark(unpark);
        }
    }

    public static final class Node extends MultiThreadedQueue<Thread> {

        private final ReentrantAreaLock lock;
        private final long[] areaAffected;
        private int areaAffectedLen;
        private final Thread thread;

        private Node(final ReentrantAreaLock lock, final long[] areaAffected, final Thread thread) {
            this.lock = lock;
            this.areaAffected = areaAffected;
            this.thread = thread;
        }

        @Override
        public String toString() {
            return "Node{" +
                "areaAffected=" + IntPairUtil.toString(this.areaAffected, 0, this.areaAffectedLen) +
                ", thread=" + this.thread +
                '}';
        }
    }
}
