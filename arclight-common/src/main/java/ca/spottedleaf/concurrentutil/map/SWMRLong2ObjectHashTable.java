package ca.spottedleaf.concurrentutil.map;

import ca.spottedleaf.concurrentutil.function.BiLongObjectConsumer;
import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.concurrentutil.util.HashUtil;
import ca.spottedleaf.concurrentutil.util.IntegerUtil;
import ca.spottedleaf.concurrentutil.util.Validate;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

// trimmed down version of SWMRHashTable
public class SWMRLong2ObjectHashTable<V> {

    protected int size;

    protected TableEntry<V>[] table;

    protected final float loadFactor;

    protected static final VarHandle SIZE_HANDLE = ConcurrentUtil.getVarHandle(SWMRLong2ObjectHashTable.class, "size", int.class);
    protected static final VarHandle TABLE_HANDLE = ConcurrentUtil.getVarHandle(SWMRLong2ObjectHashTable.class, "table", TableEntry[].class);

    /* size */

    protected final int getSizePlain() {
        return (int)SIZE_HANDLE.get(this);
    }

    protected final int getSizeOpaque() {
        return (int)SIZE_HANDLE.getOpaque(this);
    }

    protected final int getSizeAcquire() {
        return (int)SIZE_HANDLE.getAcquire(this);
    }

    protected final void setSizePlain(final int value) {
        SIZE_HANDLE.set(this, value);
    }

    protected final void setSizeOpaque(final int value) {
        SIZE_HANDLE.setOpaque(this, value);
    }

    protected final void setSizeRelease(final int value) {
        SIZE_HANDLE.setRelease(this, value);
    }

    /* table */

    protected final TableEntry<V>[] getTablePlain() {
        //noinspection unchecked
        return (TableEntry<V>[])TABLE_HANDLE.get(this);
    }

    protected final TableEntry<V>[] getTableAcquire() {
        //noinspection unchecked
        return (TableEntry<V>[])TABLE_HANDLE.getAcquire(this);
    }

    protected final void setTablePlain(final TableEntry<V>[] table) {
        TABLE_HANDLE.set(this, table);
    }

    protected final void setTableRelease(final TableEntry<V>[] table) {
        TABLE_HANDLE.setRelease(this, table);
    }

    protected static final int DEFAULT_CAPACITY = 16;
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;
    protected static final int MAXIMUM_CAPACITY = Integer.MIN_VALUE >>> 1;

    /**
     * Constructs this map with a capacity of {@code 16} and load factor of {@code 0.75f}.
     */
    public SWMRLong2ObjectHashTable() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs this map with the specified capacity and load factor of {@code 0.75f}.
     * @param capacity specified initial capacity, > 0
     */
    public SWMRLong2ObjectHashTable(final int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs this map with the specified capacity and load factor.
     * @param capacity specified capacity, > 0
     * @param loadFactor specified load factor, > 0 && finite
     */
    public SWMRLong2ObjectHashTable(final int capacity, final float loadFactor) {
        final int tableSize = getCapacityFor(capacity);

        if (loadFactor <= 0.0 || !Float.isFinite(loadFactor)) {
            throw new IllegalArgumentException("Invalid load factor: " + loadFactor);
        }

        //noinspection unchecked
        final TableEntry<V>[] table = new TableEntry[tableSize];
        this.setTablePlain(table);

        if (tableSize == MAXIMUM_CAPACITY) {
            this.threshold = -1;
        } else {
            this.threshold = getTargetCapacity(tableSize, loadFactor);
        }

        this.loadFactor = loadFactor;
    }

    /**
     * Constructs this map with a capacity of {@code 16} or the specified map's size, whichever is larger, and
     * with a load factor of {@code 0.75f}.
     * All of the specified map's entries are copied into this map.
     * @param other The specified map.
     */
    public SWMRLong2ObjectHashTable(final SWMRLong2ObjectHashTable<V> other) {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, other);
    }

    /**
     * Constructs this map with a minimum capacity of the specified capacity or the specified map's size, whichever is larger, and
     * with a load factor of {@code 0.75f}.
     * All of the specified map's entries are copied into this map.
     * @param capacity specified capacity, > 0
     * @param other The specified map.
     */
    public SWMRLong2ObjectHashTable(final int capacity, final SWMRLong2ObjectHashTable<V> other) {
        this(capacity, DEFAULT_LOAD_FACTOR, other);
    }

    /**
     * Constructs this map with a min capacity of the specified capacity or the specified map's size, whichever is larger, and
     * with the specified load factor.
     * All of the specified map's entries are copied into this map.
     * @param capacity specified capacity, > 0
     * @param loadFactor specified load factor, > 0 && finite
     * @param other The specified map.
     */
    public SWMRLong2ObjectHashTable(final int capacity, final float loadFactor, final SWMRLong2ObjectHashTable<V> other) {
        this(Math.max(Validate.notNull(other, "Null map").size(), capacity), loadFactor);
        this.putAll(other);
    }

    protected static <V> TableEntry<V> getAtIndexOpaque(final TableEntry<V>[] table, final int index) {
        // noinspection unchecked
        return (TableEntry<V>)TableEntry.TABLE_ENTRY_ARRAY_HANDLE.getOpaque(table, index);
    }

    protected static <V> void setAtIndexRelease(final TableEntry<V>[] table, final int index, final TableEntry<V> value) {
        TableEntry.TABLE_ENTRY_ARRAY_HANDLE.setRelease(table, index, value);
    }

    public final float getLoadFactor() {
        return this.loadFactor;
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

    /** Callers must still use acquire when reading the value of the entry. */
    protected final TableEntry<V> getEntryForOpaque(final long key) {
        final int hash = SWMRLong2ObjectHashTable.getHash(key);
        final TableEntry<V>[] table = this.getTableAcquire();

        for (TableEntry<V> curr = getAtIndexOpaque(table, hash & (table.length - 1)); curr != null; curr = curr.getNextOpaque()) {
            if (key == curr.key) {
                return curr;
            }
        }

        return null;
    }

    protected final TableEntry<V> getEntryForPlain(final long key) {
        final int hash = SWMRLong2ObjectHashTable.getHash(key);
        final TableEntry<V>[] table = this.getTablePlain();

        for (TableEntry<V> curr = table[hash & (table.length - 1)]; curr != null; curr = curr.getNextPlain()) {
            if (key == curr.key) {
                return curr;
            }
        }

        return null;
    }

    /* MT-Safe */

    /** must be deterministic given a key */
    protected static int getHash(final long key) {
        return (int)HashUtil.mix(key);
    }

    // rets -1 if capacity*loadFactor is too large
    protected static int getTargetCapacity(final int capacity, final float loadFactor) {
        final double ret = (double)capacity * (double)loadFactor;
        if (Double.isInfinite(ret) || ret >= ((double)Integer.MAX_VALUE)) {
            return -1;
        }

        return (int)ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        /* Make no attempt to deal with concurrent modifications */
        if (!(obj instanceof SWMRLong2ObjectHashTable<?> other)) {
            return false;
        }

        if (this.size() != other.size()) {
            return false;
        }

        final TableEntry<V>[] table = this.getTableAcquire();

        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                final V value = curr.getValueAcquire();

                final Object otherValue = other.get(curr.key);
                if (otherValue == null || (value != otherValue && value.equals(otherValue))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        /* Make no attempt to deal with concurrent modifications */
        int hash = 0;
        final TableEntry<V>[] table = this.getTableAcquire();

        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                hash += curr.hashCode();
            }
        }

        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(64);
        builder.append("SingleWriterMultiReaderHashMap:{");

        this.forEach((final long key, final V value) -> {
            builder.append("{key: \"").append(key).append("\", value: \"").append(value).append("\"}");
        });

        return builder.append('}').toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SWMRLong2ObjectHashTable<V> clone() {
        return new SWMRLong2ObjectHashTable<>(this.getTableAcquire().length, this.loadFactor, this);
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final Consumer<? super TableEntry<V>> action) {
        Validate.notNull(action, "Null action");

        final TableEntry<V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                action.accept(curr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final BiLongObjectConsumer<? super V> action) {
        Validate.notNull(action, "Null action");

        final TableEntry<V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                final V value = curr.getValueAcquire();

                action.accept(curr.key, value);
            }
        }
    }

    /**
     * Provides the specified consumer with all keys contained within this map.
     * @param action The specified consumer.
     */
    public void forEachKey(final LongConsumer action) {
        Validate.notNull(action, "Null action");

        final TableEntry<V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                action.accept(curr.key);
            }
        }
    }

    /**
     * Provides the specified consumer with all values contained within this map. Equivalent to {@code map.values().forEach(Consumer)}.
     * @param action The specified consumer.
     */
    public void forEachValue(final Consumer<? super V> action) {
        Validate.notNull(action, "Null action");

        final TableEntry<V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                final V value = curr.getValueAcquire();

                action.accept(value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public V get(final long key) {
        final TableEntry<V> entry = this.getEntryForOpaque(key);
        return entry == null ? null : entry.getValueAcquire();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(final long key) {
        // note: we need to use getValueAcquire, so that the reads from this map are ordered by acquire semantics
        return this.get(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    public V getOrDefault(final long key, final V defaultValue) {
        final TableEntry<V> entry = this.getEntryForOpaque(key);

        return entry == null ? defaultValue : entry.getValueAcquire();
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return this.getSizeAcquire();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return this.getSizeAcquire() == 0;
    }

    /* Non-MT-Safe */

    protected int threshold;

    protected final void checkResize(final int minCapacity) {
        if (minCapacity <= this.threshold || this.threshold < 0) {
            return;
        }

        final TableEntry<V>[] table = this.getTablePlain();
        int newCapacity = minCapacity >= MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : IntegerUtil.roundCeilLog2(minCapacity);
        if (newCapacity < 0) {
            newCapacity = MAXIMUM_CAPACITY;
        }
        if (newCapacity <= table.length) {
            if (newCapacity == MAXIMUM_CAPACITY) {
                return;
            }
            newCapacity = table.length << 1;
        }

        //noinspection unchecked
        final TableEntry<V>[] newTable = new TableEntry[newCapacity];
        final int indexMask = newCapacity - 1;

        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<V> entry = table[i]; entry != null; entry = entry.getNextPlain()) {
                final long key = entry.key;
                final int hash = SWMRLong2ObjectHashTable.getHash(key);
                final int index = hash & indexMask;

                /* we need to create a new entry since there could be reading threads */
                final TableEntry<V> insert = new TableEntry<>(key, entry.getValuePlain());

                final TableEntry<V> prev = newTable[index];

                newTable[index] = insert;
                insert.setNextPlain(prev);
            }
        }

        if (newCapacity == MAXIMUM_CAPACITY) {
            this.threshold = -1; /* No more resizing */
        } else {
            this.threshold = getTargetCapacity(newCapacity, this.loadFactor);
        }
        this.setTableRelease(newTable); /* use release to publish entries in table */
    }

    protected final int addToSize(final int num) {
        final int newSize = this.getSizePlain() + num;

        this.setSizeOpaque(newSize);
        this.checkResize(newSize);

        return newSize;
    }

    protected final int removeFromSize(final int num) {
        final int newSize = this.getSizePlain() - num;

        this.setSizeOpaque(newSize);

        return newSize;
    }

    protected final V put(final long key, final V value, final boolean onlyIfAbsent) {
        final TableEntry<V>[] table = this.getTablePlain();
        final int hash = SWMRLong2ObjectHashTable.getHash(key);
        final int index = hash & (table.length - 1);

        final TableEntry<V> head = table[index];
        if (head == null) {
            final TableEntry<V> insert = new TableEntry<>(key, value);
            setAtIndexRelease(table, index, insert);
            this.addToSize(1);
            return null;
        }

        for (TableEntry<V> curr = head;;) {
            if (key == curr.key) {
                if (onlyIfAbsent) {
                    return curr.getValuePlain();
                }

                final V currVal = curr.getValuePlain();
                curr.setValueRelease(value);
                return currVal;
            }

            final TableEntry<V> next = curr.getNextPlain();
            if (next != null) {
                curr = next;
                continue;
            }

            final TableEntry<V> insert = new TableEntry<>(key, value);

            curr.setNextRelease(insert);
            this.addToSize(1);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public V put(final long key, final V value) {
        Validate.notNull(value, "Null value");

        return this.put(key, value, false);
    }

    /**
     * {@inheritDoc}
     */
    public V putIfAbsent(final long key, final V value) {
        Validate.notNull(value, "Null value");

        return this.put(key, value, true);
    }

    protected final V remove(final long key, final int hash) {
        final TableEntry<V>[] table = this.getTablePlain();
        final int index = (table.length - 1) & hash;

        final TableEntry<V> head = table[index];
        if (head == null) {
            return null;
        }

        if (head.key == key) {
            setAtIndexRelease(table, index, head.getNextPlain());
            this.removeFromSize(1);

            return head.getValuePlain();
        }

        for (TableEntry<V> curr = head.getNextPlain(), prev = head; curr != null; prev = curr, curr = curr.getNextPlain()) {
            if (key == curr.key) {
                prev.setNextRelease(curr.getNextPlain());
                this.removeFromSize(1);

                return curr.getValuePlain();
            }
        }

        return null;
    }

    protected final V remove(final long key, final int hash, final V expect) {
        final TableEntry<V>[] table = this.getTablePlain();
        final int index = (table.length - 1) & hash;

        final TableEntry<V> head = table[index];
        if (head == null) {
            return null;
        }

        if (head.key == key) {
            final V val = head.value;
            if (val == expect || val.equals(expect)) {
                setAtIndexRelease(table, index, head.getNextPlain());
                this.removeFromSize(1);

                return head.getValuePlain();
            } else {
                return null;
            }
        }

        for (TableEntry<V> curr = head.getNextPlain(), prev = head; curr != null; prev = curr, curr = curr.getNextPlain()) {
            if (key == curr.key) {
                final V val = curr.value;
                if (val == expect || val.equals(expect)) {
                    prev.setNextRelease(curr.getNextPlain());
                    this.removeFromSize(1);

                    return curr.getValuePlain();
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public V remove(final long key) {
        return this.remove(key, SWMRLong2ObjectHashTable.getHash(key));
    }

    public boolean remove(final long key, final V expect) {
        return this.remove(key, SWMRLong2ObjectHashTable.getHash(key), expect) != null;
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final SWMRLong2ObjectHashTable<? extends V> map) {
        Validate.notNull(map, "Null map");

        final int size = map.size();
        this.checkResize(Math.max(this.getSizePlain() + size/2, size)); /* preemptively resize */
        map.forEach(this::put);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This call is non-atomic and the order that which entries are removed is undefined. The clear operation itself
     * is release ordered, that is, after the clear operation is performed a release fence is performed.
     * </p>
     */
    public void clear() {
        Arrays.fill(this.getTablePlain(), null);
        this.setSizeRelease(0);
    }

    public static final class TableEntry<V> {

        protected static final VarHandle TABLE_ENTRY_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(TableEntry[].class);

        protected final long key;
        protected V value;

        protected TableEntry<V> next;

        protected static final VarHandle VALUE_HANDLE = ConcurrentUtil.getVarHandle(TableEntry.class, "value", Object.class);
        protected static final VarHandle NEXT_HANDLE = ConcurrentUtil.getVarHandle(TableEntry.class, "next", TableEntry.class);

        /* value */

        protected final V getValuePlain() {
            //noinspection unchecked
            return (V)VALUE_HANDLE.get(this);
        }

        protected final V getValueAcquire() {
            //noinspection unchecked
            return (V)VALUE_HANDLE.getAcquire(this);
        }

        protected final void setValueRelease(final V to) {
            VALUE_HANDLE.setRelease(this, to);
        }

        /* next */

        protected final TableEntry<V> getNextPlain() {
            //noinspection unchecked
            return (TableEntry<V>)NEXT_HANDLE.get(this);
        }

        protected final TableEntry<V> getNextOpaque() {
            //noinspection unchecked
            return (TableEntry<V>)NEXT_HANDLE.getOpaque(this);
        }

        protected final void setNextPlain(final TableEntry<V> next) {
            NEXT_HANDLE.set(this, next);
        }

        protected final void setNextRelease(final TableEntry<V> next) {
            NEXT_HANDLE.setRelease(this, next);
        }

        protected TableEntry(final long key, final V value) {
            this.key = key;
            this.value = value;
        }

        public long getKey() {
            return this.key;
        }

        public V getValue() {
            return this.getValueAcquire();
        }
    }
}
