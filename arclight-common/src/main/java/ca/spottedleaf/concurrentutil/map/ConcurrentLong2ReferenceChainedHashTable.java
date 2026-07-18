package ca.spottedleaf.concurrentutil.map;

import ca.spottedleaf.concurrentutil.function.BiLong1Function;
import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.concurrentutil.util.HashUtil;
import ca.spottedleaf.concurrentutil.util.IntegerUtil;
import ca.spottedleaf.concurrentutil.util.ThrowUtil;
import ca.spottedleaf.concurrentutil.util.Validate;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Predicate;

/**
 * Concurrent hashtable implementation supporting mapping arbitrary {@code long} values onto non-null {@code Object}
 * values with support for multiple writer and multiple reader threads.
 *
 * <p><h3>Happens-before relationship</h3></p>
 * <p>
 * As with {@link java.util.concurrent.ConcurrentMap}, there is a happens-before relationship between actions in one thread
 * prior to writing to the map and access to the results of those actions in another thread.
 * </p>
 *
 * <p><h3>Atomicity of functional methods</h3></p>
 * <p>
 * Functional methods are functions declared in this class which possibly perform a write (remove, replace, or modify)
 * to an entry in this map as a result of invoking a function on an input parameter. For example, {@link #compute(long, BiLong1Function)},
 * {@link #merge(long, Object, BiFunction)} and {@link #removeIf(long, Predicate)} are examples of functional methods.
 * Functional methods will be performed atomically, that is, the input parameter is guaranteed to only be invoked at most
 * once per function call. The consequence of this behavior however is that a critical lock for a bin entry is held, which
 * means that if the input parameter invocation makes additional calls to write into this hash table that the result
 * is undefined and deadlock-prone.
 * </p>
 *
 * @param <V>
 * @see java.util.concurrent.ConcurrentMap
 */
public class ConcurrentLong2ReferenceChainedHashTable<V> {

    protected static final int DEFAULT_CAPACITY = 16;
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;
    protected static final int MAXIMUM_CAPACITY = Integer.MIN_VALUE >>> 1;

    protected final LongAdder size = new LongAdder();
    protected final float loadFactor;

    protected volatile TableEntry<V>[] table;

    protected static final int THRESHOLD_NO_RESIZE = -1;
    protected static final int THRESHOLD_RESIZING  = -2;
    protected volatile int threshold;
    protected static final VarHandle THRESHOLD_HANDLE = ConcurrentUtil.getVarHandle(ConcurrentLong2ReferenceChainedHashTable.class, "threshold", int.class);

    protected final int getThresholdAcquire() {
        return (int)THRESHOLD_HANDLE.getAcquire(this);
    }

    protected final int getThresholdVolatile() {
        return (int)THRESHOLD_HANDLE.getVolatile(this);
    }

    protected final void setThresholdPlain(final int threshold) {
        THRESHOLD_HANDLE.set(this, threshold);
    }

    protected final void setThresholdRelease(final int threshold) {
        THRESHOLD_HANDLE.setRelease(this, threshold);
    }

    protected final void setThresholdVolatile(final int threshold) {
        THRESHOLD_HANDLE.setVolatile(this, threshold);
    }

    protected final int compareExchangeThresholdVolatile(final int expect, final int update) {
        return (int)THRESHOLD_HANDLE.compareAndExchange(this, expect, update);
    }

    public ConcurrentLong2ReferenceChainedHashTable() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    protected static int getTargetThreshold(final int capacity, final float loadFactor) {
        final double ret = (double)capacity * (double)loadFactor;
        if (Double.isInfinite(ret) || ret >= ((double)Integer.MAX_VALUE)) {
            return THRESHOLD_NO_RESIZE;
        }

        return (int)Math.ceil(ret);
    }

    protected static int getCapacityFor(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Invalid capacity: " + capacity);
        }
        if (capacity >= MAXIMUM_CAPACITY) {
            return MAXIMUM_CAPACITY;
        }
        return IntegerUtil.roundCeilLog2(capacity);
    }

    protected ConcurrentLong2ReferenceChainedHashTable(final int capacity, final float loadFactor) {
        final int tableSize = getCapacityFor(capacity);

        if (loadFactor <= 0.0 || !Float.isFinite(loadFactor)) {
            throw new IllegalArgumentException("Invalid load factor: " + loadFactor);
        }

        if (tableSize == MAXIMUM_CAPACITY) {
            this.setThresholdPlain(THRESHOLD_NO_RESIZE);
        } else {
            this.setThresholdPlain(getTargetThreshold(tableSize, loadFactor));
        }

        this.loadFactor = loadFactor;
        // noinspection unchecked
        this.table = (TableEntry<V>[])new TableEntry[tableSize];
    }

    public static <V> ConcurrentLong2ReferenceChainedHashTable<V> createWithCapacity(final int capacity) {
        return createWithCapacity(capacity, DEFAULT_LOAD_FACTOR);
    }

    public static <V> ConcurrentLong2ReferenceChainedHashTable<V> createWithCapacity(final int capacity, final float loadFactor) {
        return new ConcurrentLong2ReferenceChainedHashTable<>(capacity, loadFactor);
    }

    public static <V> ConcurrentLong2ReferenceChainedHashTable<V> createWithExpected(final int expected) {
        return createWithExpected(expected, DEFAULT_LOAD_FACTOR);
    }

    public static <V> ConcurrentLong2ReferenceChainedHashTable<V> createWithExpected(final int expected, final float loadFactor) {
        final int capacity = (int)Math.ceil((double)expected / (double)loadFactor);

        return createWithCapacity(capacity, loadFactor);
    }

    /** must be deterministic given a key */
    protected static int getHash(final long key) {
        return (int)HashUtil.mix(key);
    }

    /**
     * Returns the load factor associated with this map.
     */
    public final float getLoadFactor() {
        return this.loadFactor;
    }

    protected static <V> TableEntry<V> getAtIndexVolatile(final TableEntry<V>[] table, final int index) {
        //noinspection unchecked
        return (TableEntry<V>)TableEntry.TABLE_ENTRY_ARRAY_HANDLE.getVolatile(table, index);
    }

    protected static <V> void setAtIndexRelease(final TableEntry<V>[] table, final int index, final TableEntry<V> value) {
        TableEntry.TABLE_ENTRY_ARRAY_HANDLE.setRelease(table, index, value);
    }

    protected static <V> void setAtIndexVolatile(final TableEntry<V>[] table, final int index, final TableEntry<V> value) {
        TableEntry.TABLE_ENTRY_ARRAY_HANDLE.setVolatile(table, index, value);
    }

    protected static <V> TableEntry<V> compareAndExchangeAtIndexVolatile(final TableEntry<V>[] table, final int index,
                                                                         final TableEntry<V> expect, final TableEntry<V> update) {
        //noinspection unchecked
        return (TableEntry<V>)TableEntry.TABLE_ENTRY_ARRAY_HANDLE.compareAndExchange(table, index, expect, update);
    }

    /**
     * Returns the possible node associated with the key, or {@code null} if there is no such node. The node
     * returned may have a {@code null} {@link TableEntry#value}, in which case the node is a placeholder for
     * a compute/computeIfAbsent call. The placeholder node should not be considered mapped in order to preserve
     * happens-before relationships between writes and reads in the map.
     */
    protected final TableEntry<V> getNode(final long key) {
        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        for (;;) {
            TableEntry<V> node = getAtIndexVolatile(table, hash & (table.length - 1));

            if (node == null) {
                // node == null
                return node;
            }

            if (node.resize) {
                table = (TableEntry<V>[])node.getValuePlain();
                continue;
            }

            for (; node != null; node = node.getNextVolatile()) {
                if (node.key == key) {
                    return node;
                }
            }

            // node == null
            return node;
        }
    }

    /**
     * Returns the currently mapped value associated with the specified key, or {@code null} if there is none.
     *
     * @param key Specified key
     */
    public V get(final long key) {
        final TableEntry<V> node = this.getNode(key);
        return node == null ? null : node.getValueVolatile();
    }

    /**
     * Returns the currently mapped value associated with the specified key, or the specified default value if there is none.
     *
     * @param key Specified key
     * @param defaultValue Specified default value
     */
    public V getOrDefault(final long key, final V defaultValue) {
        final TableEntry<V> node = this.getNode(key);
        if (node == null) {
            return defaultValue;
        }

        final V ret = node.getValueVolatile();
        if (ret == null) {
            // ret == null for nodes pre-allocated to compute() and friends
            return defaultValue;
        }

        return ret;
    }

    /**
     * Returns whether the specified key is mapped to some value.
     * @param key Specified key
     */
    public boolean containsKey(final long key) {
        // cannot use getNode, as the node may be a placeholder for compute()
        return this.get(key) != null;
    }

    /**
     * Returns whether the specified value has a key mapped to it.
     * @param value Specified value
     * @throws NullPointerException If value is null
     */
    public boolean containsValue(final V value) {
        Validate.notNull(value, "Value cannot be null");

        final NodeIterator<V> iterator = new NodeIterator<>(this.table);

        TableEntry<V> node;
        while ((node = iterator.findNext()) != null) {
            // need to use acquire here to ensure the happens-before relationship
            if (node.getValueAcquire() == value) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the number of mappings in this map.
     */
    public int size() {
        final long ret = this.size.sum();

        if (ret <= 0L) {
            return 0;
        }
        if (ret >= (long)Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int)ret;
    }

    /**
     * Returns whether this map has no mappings.
     */
    public boolean isEmpty() {
        return this.size.sum() <= 0L;
    }

    /**
     * Adds count to size and checks threshold for resizing
     */
    protected final void addSize(final long count) {
        this.size.add(count);

        final int threshold = this.getThresholdAcquire();

        if (threshold < 0L) {
            // resizing or no resizing allowed, in either cases we do not need to do anything
            return;
        }

        final long sum = this.size.sum();

        if (sum < (long)threshold) {
            return;
        }

        if (threshold != this.compareExchangeThresholdVolatile(threshold, THRESHOLD_RESIZING)) {
            // some other thread resized
            return;
        }

        // create new table
        this.resize(sum);
    }

    /**
     * Resizes table, only invoke for the thread which has successfully updated threshold to {@link #THRESHOLD_RESIZING}
     * @param sum Estimate of current mapping count, must be >= old threshold
     */
    private void resize(final long sum) {
        int capacity;

        // add 1.0, as sum may equal threshold (in which case, sum / loadFactor = current capacity)
        // adding 1.0 should at least raise the size by a factor of two due to usage of roundCeilLog2
        final double targetD = ((double)sum / (double)this.loadFactor) + 1.0;
        if (targetD >= (double)MAXIMUM_CAPACITY) {
            capacity = MAXIMUM_CAPACITY;
        } else {
            capacity = (int)Math.ceil(targetD);
            capacity = IntegerUtil.roundCeilLog2(capacity);
            if (capacity > MAXIMUM_CAPACITY) {
                capacity = MAXIMUM_CAPACITY;
            }
        }

        // create new table data

        final TableEntry<V>[] newTable = new TableEntry[capacity];
        // noinspection unchecked
        final TableEntry<V> resizeNode = new TableEntry<>(0L, (V)newTable, true);

        // transfer nodes from old table

        // does not need to be volatile read, just plain
        final TableEntry<V>[] oldTable = this.table;

        // when resizing, the old entries at bin i (where i = hash % oldTable.length) are assigned to
        // bin k in the new table (where k = hash % newTable.length)
        // since both table lengths are powers of two (specifically, newTable is a multiple of oldTable),
        // the possible number of locations in the new table to assign any given i is newTable.length/oldTable.length

        // we can build the new linked nodes for the new table by using a work array sized to newTable.length/oldTable.length
        // which holds the _last_ entry in the chain per bin

        final int capOldShift = IntegerUtil.floorLog2(oldTable.length);
        final int capDiffShift = IntegerUtil.floorLog2(capacity) - capOldShift;

        if (capDiffShift == 0) {
            throw new IllegalStateException("Resizing to same size");
        }

        final TableEntry<V>[] work = new TableEntry[1 << capDiffShift]; // typically, capDiffShift = 1

        for (int i = 0, len = oldTable.length; i < len; ++i) {
            TableEntry<V> binNode = getAtIndexVolatile(oldTable, i);

            for (;;) {
                if (binNode == null) {
                    // just need to replace the bin node, do not need to move anything
                    if (null == (binNode = compareAndExchangeAtIndexVolatile(oldTable, i, null, resizeNode))) {
                        break;
                    } // else: binNode != null, fall through
                }

                // need write lock to block other writers
                synchronized (binNode) {
                    if (binNode != (binNode = getAtIndexVolatile(oldTable, i))) {
                        continue;
                    }

                    // an important detail of resizing is that we do not need to be concerned with synchronisation on
                    // writes to the new table, as no access to any nodes on bin i on oldTable will occur until a thread
                    // sees the resizeNode
                    // specifically, as long as the resizeNode is release written there are no cases where another thread
                    // will see our writes to the new table

                    TableEntry<V> next = binNode.getNextPlain();

                    if (next == null) {
                        // simple case: do not use work array

                        // do not need to create new node, readers only need to see the state of the map at the
                        // beginning of a call, so any additions onto _next_ don't really matter
                        // additionally, the old node is replaced so that writers automatically forward to the new table,
                        // which resolves any issues
                        newTable[getHash(binNode.key) & (capacity - 1)] = binNode;
                    } else {
                        // reset for next usage
                        Arrays.fill(work, null);

                        for (TableEntry<V> curr = binNode; curr != null; curr = curr.getNextPlain()) {
                            final int newTableIdx = getHash(curr.key) & (capacity - 1);
                            final int workIdx = newTableIdx >>> capOldShift;

                            final TableEntry<V> replace = new TableEntry<>(curr.key, curr.getValuePlain());

                            final TableEntry<V> workNode = work[workIdx];
                            work[workIdx] = replace;

                            if (workNode == null) {
                                newTable[newTableIdx] = replace;
                                continue;
                            } else {
                                workNode.setNextPlain(replace);
                                continue;
                            }
                        }
                    }

                    setAtIndexRelease(oldTable, i, resizeNode);
                    break;
                }
            }
        }

        // calculate new threshold
        final int newThreshold;
        if (capacity == MAXIMUM_CAPACITY) {
            newThreshold = THRESHOLD_NO_RESIZE;
        } else {
            newThreshold = getTargetThreshold(capacity, loadFactor);
        }

        this.table = newTable;
        // finish resize operation by releasing hold on threshold
        this.setThresholdVolatile(newThreshold);
    }

    /**
     * Subtracts count from size
     */
    protected final void subSize(final long count) {
        this.size.add(-count);
    }

    /**
     * Atomically updates the value associated with {@code key} to {@code value}, or inserts a new mapping with {@code key}
     * mapped to {@code value}.
     * @param key Specified key
     * @param value Specified value
     * @throws NullPointerException If value is null
     * @return Old value previously associated with key, or {@code null} if none.
     */
    public V put(final long key, final V value) {
        Validate.notNull(value, "Value may not be null");

        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    if (null == (node = compareAndExchangeAtIndexVolatile(table, index, null, new TableEntry<>(key, value)))) {
                        // successfully inserted
                        this.addSize(1L);
                        return null;
                    } // else: node != null, fall through
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }
                    // plain reads are fine during synchronised access, as we are the only writer
                    TableEntry<V> prev = null;
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            final V ret = node.getValuePlain();
                            node.setValueVolatile(value);
                            return ret;
                        }
                    }

                    // volatile ordering ensured by addSize(), but we need release here
                    // to ensure proper ordering with reads and other writes
                    prev.setNextRelease(new TableEntry<>(key, value));
                }

                this.addSize(1L);
                return null;
            }
        }
    }

    /**
     * Atomically inserts a new mapping with {@code key} mapped to {@code value} if and only if {@code key} is not
     * currently mapped to some value.
     * @param key Specified key
     * @param value Specified value
     * @throws NullPointerException If value is null
     * @return Value currently associated with key, or {@code null} if none and {@code value} was associated.
     */
    public V putIfAbsent(final long key, final V value) {
        Validate.notNull(value, "Value may not be null");

        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    if (null == (node = compareAndExchangeAtIndexVolatile(table, index, null, new TableEntry<>(key, value)))) {
                        // successfully inserted
                        this.addSize(1L);
                        return null;
                    } // else: node != null, fall through
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                // optimise ifAbsent calls: check if first node is key before attempting lock acquire
                if (node.key == key) {
                    final V ret = node.getValueVolatile();
                    if (ret != null) {
                        return ret;
                    } // else: fall back to lock to read the node
                }

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }
                    // plain reads are fine during synchronised access, as we are the only writer
                    TableEntry<V> prev = null;
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            return node.getValuePlain();
                        }
                    }

                    // volatile ordering ensured by addSize(), but we need release here
                    // to ensure proper ordering with reads and other writes
                    prev.setNextRelease(new TableEntry<>(key, value));
                }

                this.addSize(1L);
                return null;
            }
        }
    }

    /**
     * Atomically updates the value associated with {@code key} to {@code value}, or does nothing if {@code key} is not
     * associated with a value.
     * @param key Specified key
     * @param value Specified value
     * @throws NullPointerException If value is null
     * @return Old value previously associated with key, or {@code null} if none.
     */
    public V replace(final long key, final V value) {
        Validate.notNull(value, "Value may not be null");

        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    return null;
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }

                    // plain reads are fine during synchronised access, as we are the only writer
                    for (; node != null; node = node.getNextPlain()) {
                        if (node.key == key) {
                            final V ret = node.getValuePlain();
                            node.setValueVolatile(value);
                            return ret;
                        }
                    }
                }

                return null;
            }
        }
    }

    /**
     * Atomically updates the value associated with {@code key} to {@code update} if the currently associated
     * value is reference equal to {@code expect}, otherwise does nothing.
     * @param key Specified key
     * @param expect Expected value to check current mapped value with
     * @param update Update value to replace mapped value with
     * @throws NullPointerException If value is null
     * @return If the currently mapped value is not reference equal to {@code expect}, then returns the currently mapped
     *         value. If the key is not mapped to any value, then returns {@code null}. If neither of the two cases are
     *         true, then returns {@code expect}.
     */
    public V replace(final long key, final V expect, final V update) {
        Validate.notNull(expect, "Expect may not be null");
        Validate.notNull(update, "Update may not be null");

        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    return null;
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }

                    // plain reads are fine during synchronised access, as we are the only writer
                    for (; node != null; node = node.getNextPlain()) {
                        if (node.key == key) {
                            final V ret = node.getValuePlain();

                            if (ret != expect) {
                                return ret;
                            }

                            node.setValueVolatile(update);
                            return ret;
                        }
                    }
                }

                return null;
            }
        }
    }

    /**
     * Atomically removes the mapping for the specified key and returns the value it was associated with. If the key
     * is not mapped to a value, then does nothing and returns {@code null}.
     * @param key Specified key
     * @return Old value previously associated with key, or {@code null} if none.
     */
    public V remove(final long key) {
        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    return null;
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                boolean removed = false;
                V ret = null;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }

                    TableEntry<V> prev = null;

                    // plain reads are fine during synchronised access, as we are the only writer
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            ret = node.getValuePlain();
                            removed = true;

                            // volatile ordering ensured by addSize(), but we need release here
                            // to ensure proper ordering with reads and other writes
                            if (prev == null) {
                                setAtIndexRelease(table, index, node.getNextPlain());
                            } else {
                                prev.setNextRelease(node.getNextPlain());
                            }

                            break;
                        }
                    }
                }

                if (removed) {
                    this.subSize(1L);
                }

                return ret;
            }
        }
    }

    /**
     * Atomically removes the mapping for the specified key if it is mapped to {@code expect} and returns {@code expect}. If the key
     * is not mapped to a value, then does nothing and returns {@code null}. If the key is mapped to a value that is not reference
     * equal to {@code expect}, then returns that value.
     * @param key Specified key
     * @param expect Specified expected value
     * @return The specified expected value if the key was mapped to {@code expect}. If
     *         the key is not mapped to any value, then returns {@code null}. If neither of those cases are true,
     *         then returns the current (non-null) mapped value for key.
     */
    public V remove(final long key, final V expect) {
        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    return null;
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                boolean removed = false;
                V ret = null;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }

                    TableEntry<V> prev = null;

                    // plain reads are fine during synchronised access, as we are the only writer
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            ret = node.getValuePlain();
                            if (ret == expect) {
                                removed = true;

                                // volatile ordering ensured by addSize(), but we need release here
                                // to ensure proper ordering with reads and other writes
                                if (prev == null) {
                                    setAtIndexRelease(table, index, node.getNextPlain());
                                } else {
                                    prev.setNextRelease(node.getNextPlain());
                                }
                            }
                            break;
                        }
                    }
                }

                if (removed) {
                    this.subSize(1L);
                }

                return ret;
            }
        }
    }

    /**
     * Atomically removes the mapping for the specified key the predicate returns true for its currently mapped value. If the key
     * is not mapped to a value, then does nothing and returns {@code null}.
     *
     * <p>
     * This function is a "functional methods" as defined by {@link ConcurrentLong2ReferenceChainedHashTable}.
     * </p>
     *
     * @param key Specified key
     * @param predicate Specified predicate
     * @throws NullPointerException If predicate is null
     * @return The specified expected value if the key was mapped to {@code expect}. If
     *         the key is not mapped to any value, then returns {@code null}. If neither of those cases are true,
     *         then returns the current (non-null) mapped value for key.
     */
    public V removeIf(final long key, final Predicate<? super V> predicate) {
        Validate.notNull(predicate, "Predicate may not be null");

        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    return null;
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                boolean removed = false;
                V ret = null;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }

                    TableEntry<V> prev = null;

                    // plain reads are fine during synchronised access, as we are the only writer
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            ret = node.getValuePlain();
                            if (predicate.test(ret)) {
                                removed = true;

                                // volatile ordering ensured by addSize(), but we need release here
                                // to ensure proper ordering with reads and other writes
                                if (prev == null) {
                                    setAtIndexRelease(table, index, node.getNextPlain());
                                } else {
                                    prev.setNextRelease(node.getNextPlain());
                                }
                            }
                            break;
                        }
                    }
                }

                if (removed) {
                    this.subSize(1L);
                }

                return ret;
            }
        }
    }

    /**
     * See {@link java.util.concurrent.ConcurrentMap#compute(Object, BiFunction)}
     * <p>
     * This function is a "functional methods" as defined by {@link ConcurrentLong2ReferenceChainedHashTable}.
     * </p>
     */
    public V compute(final long key, final BiLong1Function<? super V, ? extends V> function) {
        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                V ret = null;
                if (node == null) {
                    final TableEntry<V> insert = new TableEntry<>(key, null);

                    boolean added = false;

                    synchronized (insert) {
                        if (null == (node = compareAndExchangeAtIndexVolatile(table, index, null, insert))) {
                            try {
                                ret = function.apply(key, null);
                            } catch (final Throwable throwable) {
                                setAtIndexVolatile(table, index, null);
                                ThrowUtil.throwUnchecked(throwable);
                                // unreachable
                                return null;
                            }

                            if (ret == null) {
                                setAtIndexVolatile(table, index, null);
                                return ret;
                            } else {
                                // volatile ordering ensured by addSize(), but we need release here
                                // to ensure proper ordering with reads and other writes
                                insert.setValueRelease(ret);
                                added = true;
                            }
                        } // else: node != null, fall through
                    }

                    if (added) {
                        this.addSize(1L);
                        return ret;
                    }
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                boolean removed = false;
                boolean added = false;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }
                    // plain reads are fine during synchronised access, as we are the only writer
                    TableEntry<V> prev = null;
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            final V old = node.getValuePlain();

                            final V computed = function.apply(key, old);

                            if (computed != null) {
                                node.setValueVolatile(computed);
                                return computed;
                            }

                            // volatile ordering ensured by addSize(), but we need release here
                            // to ensure proper ordering with reads and other writes
                            if (prev == null) {
                                setAtIndexRelease(table, index, node.getNextPlain());
                            } else {
                                prev.setNextRelease(node.getNextPlain());
                            }

                            removed = true;
                            break;
                        }
                    }

                    if (!removed) {
                        final V computed = function.apply(key, null);
                        if (computed != null) {
                            // volatile ordering ensured by addSize(), but we need release here
                            // to ensure proper ordering with reads and other writes
                            prev.setNextRelease(new TableEntry<>(key, computed));
                            ret = computed;
                            added = true;
                        }
                    }
                }

                if (removed) {
                    this.subSize(1L);
                }
                if (added) {
                    this.addSize(1L);
                }

                return ret;
            }
        }
    }

    /**
     * See {@link java.util.concurrent.ConcurrentMap#computeIfAbsent(Object, Function)}
     * <p>
     * This function is a "functional methods" as defined by {@link ConcurrentLong2ReferenceChainedHashTable}.
     * </p>
     */
    public V computeIfAbsent(final long key, final LongFunction<? extends V> function) {
        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                V ret = null;
                if (node == null) {
                    final TableEntry<V> insert = new TableEntry<>(key, null);

                    boolean added = false;

                    synchronized (insert) {
                        if (null == (node = compareAndExchangeAtIndexVolatile(table, index, null, insert))) {
                            try {
                                ret = function.apply(key);
                            } catch (final Throwable throwable) {
                                setAtIndexVolatile(table, index, null);
                                ThrowUtil.throwUnchecked(throwable);
                                // unreachable
                                return null;
                            }

                            if (ret == null) {
                                setAtIndexVolatile(table, index, null);
                                return null;
                            } else {
                                // volatile ordering ensured by addSize(), but we need release here
                                // to ensure proper ordering with reads and other writes
                                insert.setValueRelease(ret);
                                added = true;
                            }
                        } // else: node != null, fall through
                    }

                    if (added) {
                        this.addSize(1L);
                        return ret;
                    }
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                // optimise ifAbsent calls: check if first node is key before attempting lock acquire
                if (node.key == key) {
                    ret = node.getValueVolatile();
                    if (ret != null) {
                        return ret;
                    } // else: fall back to lock to read the node
                }

                boolean added = false;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }
                    // plain reads are fine during synchronised access, as we are the only writer
                    TableEntry<V> prev = null;
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            ret = node.getValuePlain();
                            return ret;
                        }
                    }

                    final V computed = function.apply(key);
                    if (computed != null) {
                        // volatile ordering ensured by addSize(), but we need release here
                        // to ensure proper ordering with reads and other writes
                        prev.setNextRelease(new TableEntry<>(key, computed));
                        ret = computed;
                        added = true;
                    }
                }

                if (added) {
                    this.addSize(1L);
                }

                return ret;
            }
        }
    }

    /**
     * See {@link java.util.concurrent.ConcurrentMap#computeIfPresent(Object, BiFunction)}
     * <p>
     * This function is a "functional methods" as defined by {@link ConcurrentLong2ReferenceChainedHashTable}.
     * </p>
     */
    public V computeIfPresent(final long key, final BiLong1Function<? super V, ? extends V> function) {
        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    return null;
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                boolean removed = false;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }
                    // plain reads are fine during synchronised access, as we are the only writer
                    TableEntry<V> prev = null;
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            final V old = node.getValuePlain();

                            final V computed = function.apply(key, old);

                            if (computed != null) {
                                node.setValueVolatile(computed);
                                return computed;
                            }

                            // volatile ordering ensured by addSize(), but we need release here
                            // to ensure proper ordering with reads and other writes
                            if (prev == null) {
                                setAtIndexRelease(table, index, node.getNextPlain());
                            } else {
                                prev.setNextRelease(node.getNextPlain());
                            }

                            removed = true;
                            break;
                        }
                    }
                }

                if (removed) {
                    this.subSize(1L);
                }

                return null;
            }
        }
    }

    /**
     * See {@link java.util.concurrent.ConcurrentMap#merge(Object, Object, BiFunction)}
     * <p>
     * This function is a "functional methods" as defined by {@link ConcurrentLong2ReferenceChainedHashTable}.
     * </p>
     */
    public V merge(final long key, final V def, final BiFunction<? super V, ? super V, ? extends V> function) {
        Validate.notNull(def, "Default value may not be null");

        final int hash = getHash(key);

        TableEntry<V>[] table = this.table;
        table_loop:
        for (;;) {
            final int index = hash & (table.length - 1);

            TableEntry<V> node = getAtIndexVolatile(table, index);
            node_loop:
            for (;;) {
                if (node == null) {
                    if (null == (node = compareAndExchangeAtIndexVolatile(table, index, null, new TableEntry<>(key, def)))) {
                        // successfully inserted
                        this.addSize(1L);
                        return def;
                    } // else: node != null, fall through
                }

                if (node.resize) {
                    table = (TableEntry<V>[])node.getValuePlain();
                    continue table_loop;
                }

                boolean removed = false;
                boolean added = false;
                V ret = null;

                synchronized (node) {
                    if (node != (node = getAtIndexVolatile(table, index))) {
                        continue node_loop;
                    }
                    // plain reads are fine during synchronised access, as we are the only writer
                    TableEntry<V> prev = null;
                    for (; node != null; prev = node, node = node.getNextPlain()) {
                        if (node.key == key) {
                            final V old = node.getValuePlain();

                            final V computed = function.apply(old, def);

                            if (computed != null) {
                                node.setValueVolatile(computed);
                                return computed;
                            }

                            // volatile ordering ensured by addSize(), but we need release here
                            // to ensure proper ordering with reads and other writes
                            if (prev == null) {
                                setAtIndexRelease(table, index, node.getNextPlain());
                            } else {
                                prev.setNextRelease(node.getNextPlain());
                            }

                            removed = true;
                            break;
                        }
                    }

                    if (!removed) {
                        // volatile ordering ensured by addSize(), but we need release here
                        // to ensure proper ordering with reads and other writes
                        prev.setNextRelease(new TableEntry<>(key, def));
                        ret = def;
                        added = true;
                    }
                }

                if (removed) {
                    this.subSize(1L);
                }
                if (added) {
                    this.addSize(1L);
                }

                return ret;
            }
        }
    }

    /**
     * Removes at least all entries currently mapped at the beginning of this call. May not remove entries added during
     * this call. As a result, only if this map is not modified during the call, that all entries will be removed by
     * the end of the call.
     *
     * <p>
     * This function is not atomic.
     * </p>
     */
    public void clear() {
        // it is possible to optimise this to directly interact with the table,
        // but we do need to be careful when interacting with resized tables,
        // and the NodeIterator already does this logic
        final NodeIterator<V> nodeIterator = new NodeIterator<>(this.table);

        TableEntry<V> node;
        while ((node = nodeIterator.findNext()) != null) {
            this.remove(node.key);
        }
    }

    /**
     * Returns an iterator over the entries in this map. The iterator is only guaranteed to see entries that were
     * added before the beginning of this call, but it may see entries added during.
     */
    public Iterator<TableEntry<V>> entryIterator() {
        return new EntryIterator<>(this);
    }

    /**
     * Returns an iterator over the keys in this map. The iterator is only guaranteed to see keys that were
     * added before the beginning of this call, but it may see keys added during.
     */
    public PrimitiveIterator.OfLong keyIterator() {
        return new KeyIterator<>(this);
    }

    /**
     * Returns an iterator over the values in this map. The iterator is only guaranteed to see values that were
     * added before the beginning of this call, but it may see values added during.
     */
    public Iterator<V> valueIterator() {
        return new ValueIterator<>(this);
    }

    protected static final class EntryIterator<V> extends BaseIteratorImpl<V, TableEntry<V>> {

        protected EntryIterator(final ConcurrentLong2ReferenceChainedHashTable<V> map) {
            super(map);
        }

        @Override
        public TableEntry<V> next() throws NoSuchElementException {
            return this.nextNode();
        }

        @Override
        public void forEachRemaining(final Consumer<? super TableEntry<V>> action) {
            Validate.notNull(action, "Action may not be null");
            while (this.hasNext()) {
                action.accept(this.next());
            }
        }
    }

    protected static final class KeyIterator<V> extends BaseIteratorImpl<V, Long> implements PrimitiveIterator.OfLong {

        protected KeyIterator(final ConcurrentLong2ReferenceChainedHashTable<V> map) {
            super(map);
        }

        @Override
        public Long next() throws NoSuchElementException {
            return Long.valueOf(this.nextNode().key);
        }

        @Override
        public long nextLong() {
            return this.nextNode().key;
        }

        @Override
        public void forEachRemaining(final Consumer<? super Long> action) {
            Validate.notNull(action, "Action may not be null");

            if (action instanceof LongConsumer longConsumer) {
                this.forEachRemaining(longConsumer);
                return;
            }

            while (this.hasNext()) {
                action.accept(this.next());
            }
        }

        @Override
        public void forEachRemaining(final LongConsumer action) {
            Validate.notNull(action, "Action may not be null");
            while (this.hasNext()) {
                action.accept(this.nextLong());
            }
        }
    }

    protected static final class ValueIterator<V> extends BaseIteratorImpl<V, V> {

        protected ValueIterator(final ConcurrentLong2ReferenceChainedHashTable<V> map) {
            super(map);
        }

        @Override
        public V next() throws NoSuchElementException {
            return this.nextNode().getValueVolatile();
        }

        @Override
        public void forEachRemaining(final Consumer<? super V> action) {
            Validate.notNull(action, "Action may not be null");
            while (this.hasNext()) {
                action.accept(this.next());
            }
        }
    }

    protected static abstract class BaseIteratorImpl<V, T> extends NodeIterator<V> implements Iterator<T> {

        protected final ConcurrentLong2ReferenceChainedHashTable<V> map;
        protected TableEntry<V> lastReturned;
        protected TableEntry<V> nextToReturn;

        protected BaseIteratorImpl(final ConcurrentLong2ReferenceChainedHashTable<V> map) {
            super(map.table);
            this.map = map;
        }

        @Override
        public final boolean hasNext() {
            if (this.nextToReturn != null) {
                return true;
            }

            return (this.nextToReturn = this.findNext()) != null;
        }

        protected final TableEntry<V> nextNode() throws NoSuchElementException {
            TableEntry<V> ret = this.nextToReturn;
            if (ret != null) {
                this.lastReturned = ret;
                this.nextToReturn = null;
                return ret;
            }
            ret = this.findNext();
            if (ret != null) {
                this.lastReturned = ret;
                return ret;
            }
            throw new NoSuchElementException();
        }

        @Override
        public final void remove() {
            final TableEntry<V> lastReturned = this.nextToReturn;
            if (lastReturned == null) {
                throw new NoSuchElementException();
            }
            this.lastReturned = null;
            this.map.remove(lastReturned.key);
        }

        @Override
        public abstract T next() throws NoSuchElementException;

        // overwritten by subclasses to avoid indirection on hasNext() and next()
        @Override
        public abstract void forEachRemaining(final Consumer<? super T> action);
    }

    protected static class NodeIterator<V> {

        protected TableEntry<V>[] currentTable;
        protected ResizeChain<V> resizeChain;
        protected TableEntry<V> last;
        protected int nextBin;
        protected int increment;

        protected NodeIterator(final TableEntry<V>[] baseTable) {
            this.currentTable = baseTable;
            this.increment = 1;
        }

        private TableEntry<V>[] pullResizeChain(final int index) {
            final ResizeChain<V> resizeChain = this.resizeChain;
            if (resizeChain == null) {
                this.currentTable = null;
                return null;
            }

            final ResizeChain<V> prevChain = resizeChain.prev;
            this.resizeChain = prevChain;
            if (prevChain == null) {
                this.currentTable = null;
                return null;
            }

            final TableEntry<V>[] newTable = prevChain.table;

            // we recover the original index by modding by the new table length, as the increments applied to the index
            // are a multiple of the new table's length
            int newIdx = index & (newTable.length - 1);

            // the increment is always the previous table's length
            final ResizeChain<V> nextPrevChain = prevChain.prev;
            final int increment;
            if (nextPrevChain == null) {
                increment = 1;
            } else {
                increment = nextPrevChain.table.length;
            }

            // done with the upper table, so we can skip the resize node
            newIdx += increment;

            this.increment = increment;
            this.nextBin = newIdx;
            this.currentTable = newTable;

            return newTable;
        }

        private TableEntry<V>[] pushResizeChain(final TableEntry<V>[] table, final TableEntry<V> entry) {
            final ResizeChain<V> chain = this.resizeChain;

            if (chain == null) {
                final TableEntry<V>[] nextTable = (TableEntry<V>[])entry.getValuePlain();

                final ResizeChain<V> oldChain = new ResizeChain<>(table, null, null);
                final ResizeChain<V> currChain = new ResizeChain<>(nextTable, oldChain, null);
                oldChain.next = currChain;

                this.increment = table.length;
                this.resizeChain = currChain;
                this.currentTable = nextTable;

                return nextTable;
            } else {
                ResizeChain<V> currChain = chain.next;
                if (currChain == null) {
                    final TableEntry<V>[] ret = (TableEntry<V>[])entry.getValuePlain();
                    currChain = new ResizeChain<>(ret, chain, null);
                    chain.next = currChain;

                    this.increment = table.length;
                    this.resizeChain = currChain;
                    this.currentTable = ret;

                    return ret;
                } else {
                    this.increment = table.length;
                    this.resizeChain = currChain;
                    return this.currentTable = currChain.table;
                }
            }
        }

        protected final TableEntry<V> findNext() {
            for (;;) {
                final TableEntry<V> last = this.last;
                if (last != null) {
                    final TableEntry<V> next = last.getNextVolatile();
                    if (next != null) {
                        this.last = next;
                        if (next.getValuePlain() == null) {
                            // compute() node not yet available
                            continue;
                        }
                        return next;
                    }
                }

                TableEntry<V>[] table = this.currentTable;

                if (table == null) {
                    return null;
                }

                int idx = this.nextBin;
                int increment = this.increment;
                for (;;) {
                    if (idx >= table.length) {
                        table = this.pullResizeChain(idx);
                        idx = this.nextBin;
                        increment = this.increment;
                        if (table != null) {
                            continue;
                        } else {
                            this.last = null;
                            return null;
                        }
                    }

                    final TableEntry<V> entry = getAtIndexVolatile(table, idx);
                    if (entry == null) {
                        idx += increment;
                        continue;
                    }

                    if (entry.resize) {
                        // push onto resize chain
                        table = this.pushResizeChain(table, entry);
                        increment = this.increment;
                        continue;
                    }

                    this.last = entry;
                    this.nextBin = idx + increment;
                    if (entry.getValuePlain() != null) {
                        return entry;
                    } else {
                        // compute() node not yet available
                        break;
                    }
                }
            }
        }

        protected static final class ResizeChain<V> {

            protected final TableEntry<V>[] table;
            protected final ResizeChain<V> prev;
            protected ResizeChain<V> next;

            protected ResizeChain(final TableEntry<V>[] table, final ResizeChain<V> prev, final ResizeChain<V> next) {
                this.table = table;
                this.prev = prev;
                this.next = next;
            }
        }
    }

    public static final class TableEntry<V> {

        protected static final VarHandle TABLE_ENTRY_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(TableEntry[].class);

        protected final boolean resize;

        protected final long key;

        protected volatile V value;
        protected static final VarHandle VALUE_HANDLE = ConcurrentUtil.getVarHandle(TableEntry.class, "value", Object.class);

        protected final V getValuePlain() {
            //noinspection unchecked
            return (V)VALUE_HANDLE.get(this);
        }

        protected final V getValueAcquire() {
            //noinspection unchecked
            return (V)VALUE_HANDLE.getAcquire(this);
        }

        protected final V getValueVolatile() {
            //noinspection unchecked
            return (V)VALUE_HANDLE.getVolatile(this);
        }

        protected final void setValuePlain(final V value) {
            VALUE_HANDLE.set(this, (Object)value);
        }

        protected final void setValueRelease(final V value) {
            VALUE_HANDLE.setRelease(this, (Object)value);
        }

        protected final void setValueVolatile(final V value) {
            VALUE_HANDLE.setVolatile(this, (Object)value);
        }

        protected volatile TableEntry<V> next;
        protected static final VarHandle NEXT_HANDLE = ConcurrentUtil.getVarHandle(TableEntry.class, "next", TableEntry.class);

        protected final TableEntry<V> getNextPlain() {
            //noinspection unchecked
            return (TableEntry<V>)NEXT_HANDLE.get(this);
        }

        protected final TableEntry<V> getNextVolatile() {
            //noinspection unchecked
            return (TableEntry<V>)NEXT_HANDLE.getVolatile(this);
        }

        protected final void setNextPlain(final TableEntry<V> next) {
            NEXT_HANDLE.set(this, next);
        }

        protected final void setNextRelease(final TableEntry<V> next) {
            NEXT_HANDLE.setRelease(this, next);
        }

        protected final void setNextVolatile(final TableEntry<V> next) {
            NEXT_HANDLE.setVolatile(this, next);
        }

        public TableEntry(final long key, final V value) {
            this.resize = false;
            this.key = key;
            this.setValuePlain(value);
        }

        public TableEntry(final long key, final V value, final boolean resize) {
            this.resize = resize;
            this.key = key;
            this.setValuePlain(value);
        }

        public long getKey() {
            return this.key;
        }

        public V getValue() {
            return this.getValueVolatile();
        }
    }
}
