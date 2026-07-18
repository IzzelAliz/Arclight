package ca.spottedleaf.concurrentutil.map;

import ca.spottedleaf.concurrentutil.util.CollectionUtil;
import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.concurrentutil.util.HashUtil;
import ca.spottedleaf.concurrentutil.util.IntegerUtil;
import ca.spottedleaf.concurrentutil.util.Validate;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * <p>
 * Note: Not really tested, use at your own risk.
 * </p>
 * This map is safe for reading from multiple threads, however it is only safe to write from a single thread.
 * {@code null} keys or values are not permitted. Writes to values in this map are guaranteed to be ordered by release semantics,
 * however immediate visibility to other threads is not guaranteed. However, writes are guaranteed to be made visible eventually.
 * Reads are ordered by acquire semantics.
 * <p>
 * Iterators cannot be modified concurrently, and its backing map cannot be modified concurrently. There is no
 * fast-fail attempt made by iterators, thus modifying the iterator's backing map while iterating will have undefined
 * behaviour.
 * </p>
 * <p>
 * Subclasses should override {@link #clone()} to return correct instances of this class.
 * </p>
 * @param <K> {@inheritDoc}
 * @param <V> {@inheritDoc}
 */
public class SWMRHashTable<K, V> implements Map<K, V>, Iterable<Map.Entry<K, V>> {

    protected int size;

    protected TableEntry<K, V>[] table;

    protected final float loadFactor;

    protected static final VarHandle SIZE_HANDLE = ConcurrentUtil.getVarHandle(SWMRHashTable.class, "size", int.class);
    protected static final VarHandle TABLE_HANDLE = ConcurrentUtil.getVarHandle(SWMRHashTable.class, "table", TableEntry[].class);

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

    protected final TableEntry<K, V>[] getTablePlain() {
        //noinspection unchecked
        return (TableEntry<K, V>[])TABLE_HANDLE.get(this);
    }

    protected final TableEntry<K, V>[] getTableAcquire() {
        //noinspection unchecked
        return (TableEntry<K, V>[])TABLE_HANDLE.getAcquire(this);
    }

    protected final void setTablePlain(final TableEntry<K, V>[] table) {
        TABLE_HANDLE.set(this, table);
    }

    protected final void setTableRelease(final TableEntry<K, V>[] table) {
        TABLE_HANDLE.setRelease(this, table);
    }

    protected static final int DEFAULT_CAPACITY = 16;
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;
    protected static final int MAXIMUM_CAPACITY = Integer.MIN_VALUE >>> 1;

    /**
     * Constructs this map with a capacity of {@code 16} and load factor of {@code 0.75f}.
     */
    public SWMRHashTable() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs this map with the specified capacity and load factor of {@code 0.75f}.
     * @param capacity specified initial capacity, > 0
     */
    public SWMRHashTable(final int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs this map with the specified capacity and load factor.
     * @param capacity specified capacity, > 0
     * @param loadFactor specified load factor, > 0 && finite
     */
    public SWMRHashTable(final int capacity, final float loadFactor) {
        final int tableSize = getCapacityFor(capacity);

        if (loadFactor <= 0.0 || !Float.isFinite(loadFactor)) {
            throw new IllegalArgumentException("Invalid load factor: " + loadFactor);
        }

        //noinspection unchecked
        final TableEntry<K, V>[] table = new TableEntry[tableSize];
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
    public SWMRHashTable(final Map<K, V> other) {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, other);
    }

    /**
     * Constructs this map with a minimum capacity of the specified capacity or the specified map's size, whichever is larger, and
     * with a load factor of {@code 0.75f}.
     * All of the specified map's entries are copied into this map.
     * @param capacity specified capacity, > 0
     * @param other The specified map.
     */
    public SWMRHashTable(final int capacity, final Map<K, V> other) {
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
    public SWMRHashTable(final int capacity, final float loadFactor, final Map<K, V> other) {
        this(Math.max(Validate.notNull(other, "Null map").size(), capacity), loadFactor);
        this.putAll(other);
    }

    protected static <K, V> TableEntry<K, V> getAtIndexOpaque(final TableEntry<K, V>[] table, final int index) {
        // noinspection unchecked
        return (TableEntry<K, V>)TableEntry.TABLE_ENTRY_ARRAY_HANDLE.getOpaque(table, index);
    }

    protected static <K, V> void setAtIndexRelease(final TableEntry<K, V>[] table, final int index, final TableEntry<K, V> value) {
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
    protected final TableEntry<K, V> getEntryForOpaque(final K key) {
        final int hash = SWMRHashTable.getHash(key);
        final TableEntry<K, V>[] table = this.getTableAcquire();

        for (TableEntry<K, V> curr = getAtIndexOpaque(table, hash & (table.length - 1)); curr != null; curr = curr.getNextOpaque()) {
            if (hash == curr.hash && (key == curr.key || curr.key.equals(key))) {
                return curr;
            }
        }

        return null;
    }

    protected final TableEntry<K, V> getEntryForPlain(final K key) {
        final int hash = SWMRHashTable.getHash(key);
        final TableEntry<K, V>[] table = this.getTablePlain();

        for (TableEntry<K, V> curr = table[hash & (table.length - 1)]; curr != null; curr = curr.getNextPlain()) {
            if (hash == curr.hash && (key == curr.key || curr.key.equals(key))) {
                return curr;
            }
        }

        return null;
    }

    /* MT-Safe */

    /** must be deterministic given a key */
    private static int getHash(final Object key) {
        int hash = key == null ? 0 : key.hashCode();
        return HashUtil.mix(hash);
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
        if (!(obj instanceof Map<?, ?> other)) {
            return false;
        }

        if (this.size() != other.size()) {
            return false;
        }

        final TableEntry<K, V>[] table = this.getTableAcquire();

        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
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
        final TableEntry<K, V>[] table = this.getTableAcquire();

        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
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
        builder.append("SWMRHashTable:{");

        this.forEach((final K key, final V value) -> {
            builder.append("{key: \"").append(key).append("\", value: \"").append(value).append("\"}");
        });

        return builder.append('}').toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SWMRHashTable<K, V> clone() {
        return new SWMRHashTable<>(this.getTableAcquire().length, this.loadFactor, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new EntryIterator<>(this.getTableAcquire(), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEach(final Consumer<? super Entry<K, V>> action) {
        Validate.notNull(action, "Null action");

        final TableEntry<K, V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                action.accept(curr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        Validate.notNull(action, "Null action");

        final TableEntry<K, V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                final V value = curr.getValueAcquire();

                action.accept(curr.key, value);
            }
        }
    }

    /**
     * Provides the specified consumer with all keys contained within this map.
     * @param action The specified consumer.
     */
    public void forEachKey(final Consumer<? super K> action) {
        Validate.notNull(action, "Null action");

        final TableEntry<K, V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
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

        final TableEntry<K, V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                final V value = curr.getValueAcquire();

                action.accept(value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(final Object key) {
        Validate.notNull(key, "Null key");

        //noinspection unchecked
        final TableEntry<K, V> entry = this.getEntryForOpaque((K)key);
        return entry == null ? null : entry.getValueAcquire();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key) {
        Validate.notNull(key, "Null key");

        // note: we need to use getValueAcquire, so that the reads from this map are ordered by acquire semantics
        return this.get(key) != null;
    }

    /**
     * Returns {@code true} if this map contains an entry with the specified key and value at some point during this call.
     * @param key The specified key.
     * @param value The specified value.
     * @return {@code true} if this map contains an entry with the specified key and value.
     */
    public boolean contains(final Object key, final Object value) {
        Validate.notNull(key, "Null key");

        //noinspection unchecked
        final TableEntry<K, V> entry = this.getEntryForOpaque((K)key);

        if (entry == null) {
            return false;
        }

        final V entryVal = entry.getValueAcquire();
        return entryVal == value || entryVal.equals(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value) {
        Validate.notNull(value, "Null value");

        final TableEntry<K, V>[] table = this.getTableAcquire();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = getAtIndexOpaque(table, i); curr != null; curr = curr.getNextOpaque()) {
                final V currVal = curr.getValueAcquire();
                if (currVal == value || currVal.equals(value)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        Validate.notNull(key, "Null key");

        //noinspection unchecked
        final TableEntry<K, V> entry = this.getEntryForOpaque((K)key);

        return entry == null ? defaultValue : entry.getValueAcquire();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return this.getSizeAcquire();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return this.getSizeAcquire() == 0;
    }

    protected KeySet<K, V> keyset;
    protected ValueCollection<K, V> values;
    protected EntrySet<K, V> entrySet;

    @Override
    public Set<K> keySet() {
        return this.keyset == null ? this.keyset = new KeySet<>(this) : this.keyset;
    }

    @Override
    public Collection<V> values() {
        return this.values == null ? this.values = new ValueCollection<>(this) : this.values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.entrySet == null ? this.entrySet = new EntrySet<>(this) : this.entrySet;
    }

    /* Non-MT-Safe */

    protected int threshold;

    protected final void checkResize(final int minCapacity) {
        if (minCapacity <= this.threshold || this.threshold < 0) {
            return;
        }

        final TableEntry<K, V>[] table = this.getTablePlain();
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
        final TableEntry<K, V>[] newTable = new TableEntry[newCapacity];
        final int indexMask = newCapacity - 1;

        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> entry = table[i]; entry != null; entry = entry.getNextPlain()) {
                final int hash = entry.hash;
                final int index = hash & indexMask;

                /* we need to create a new entry since there could be reading threads */
                final TableEntry<K, V> insert = new TableEntry<>(hash, entry.key, entry.getValuePlain());

                final TableEntry<K, V> prev = newTable[index];

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

    /* Cannot be used to perform downsizing */
    protected final int removeFromSizePlain(final int num) {
        final int newSize = this.getSizePlain() - num;

        this.setSizePlain(newSize);

        return newSize;
    }

    protected final V put(final K key, final V value, final boolean onlyIfAbsent) {
        final TableEntry<K, V>[] table = this.getTablePlain();
        final int hash = SWMRHashTable.getHash(key);
        final int index = hash & (table.length - 1);

        final TableEntry<K, V> head = table[index];
        if (head == null) {
            final TableEntry<K, V> insert = new TableEntry<>(hash, key, value);
            setAtIndexRelease(table, index, insert);
            this.addToSize(1);
            return null;
        }

        for (TableEntry<K, V> curr = head;;) {
            if (curr.hash == hash && (key == curr.key || curr.key.equals(key))) {
                if (onlyIfAbsent) {
                    return curr.getValuePlain();
                }

                final V currVal = curr.getValuePlain();
                curr.setValueRelease(value);
                return currVal;
            }

            final TableEntry<K, V> next = curr.getNextPlain();
            if (next != null) {
                curr = next;
                continue;
            }

            final TableEntry<K, V> insert = new TableEntry<>(hash, key, value);

            curr.setNextRelease(insert);
            this.addToSize(1);
            return null;
        }
    }

    /**
     * Removes a key-value pair from this map if the specified predicate returns true. The specified predicate is
     * tested with every entry in this map. Returns the number of key-value pairs removed.
     * @param predicate The predicate to test key-value pairs against.
     * @return The total number of key-value pairs removed from this map.
     */
    public int removeIf(final BiPredicate<K, V> predicate) {
        Validate.notNull(predicate, "Null predicate");

        int removed = 0;

        final TableEntry<K, V>[] table = this.getTablePlain();

        bin_iteration_loop:
        for (int i = 0, len = table.length; i < len; ++i) {
            TableEntry<K, V> curr = table[i];
            if (curr == null) {
                continue;
            }

            /* Handle bin nodes first */
            while (predicate.test(curr.key, curr.getValuePlain())) {
                ++removed;
                this.removeFromSizePlain(1); /* required in case predicate throws an exception */

                setAtIndexRelease(table, i, curr = curr.getNextPlain());

                if (curr == null) {
                    continue bin_iteration_loop;
                }
            }

            TableEntry<K, V> prev;

            /* curr at this point is the bin node */

            for (prev = curr, curr = curr.getNextPlain(); curr != null;) {
                /* If we want to remove, then we should hold prev, as it will be a valid entry to link on */
                if (predicate.test(curr.key, curr.getValuePlain())) {
                    ++removed;
                    this.removeFromSizePlain(1); /* required in case predicate throws an exception */

                    prev.setNextRelease(curr = curr.getNextPlain());
                } else {
                    prev = curr;
                    curr = curr.getNextPlain();
                }
            }
        }

        return removed;
    }

    /**
     * Removes a key-value pair from this map if the specified predicate returns true. The specified predicate is
     * tested with every entry in this map. Returns the number of key-value pairs removed.
     * @param predicate The predicate to test key-value pairs against.
     * @return The total number of key-value pairs removed from this map.
     */
    public int removeEntryIf(final Predicate<? super Entry<K, V>> predicate) {
        Validate.notNull(predicate, "Null predicate");

        int removed = 0;

        final TableEntry<K, V>[] table = this.getTablePlain();

        bin_iteration_loop:
        for (int i = 0, len = table.length; i < len; ++i) {
            TableEntry<K, V> curr = table[i];
            if (curr == null) {
                continue;
            }

            /* Handle bin nodes first */
            while (predicate.test(curr)) {
                ++removed;
                this.removeFromSizePlain(1); /* required in case predicate throws an exception */

                setAtIndexRelease(table, i, curr = curr.getNextPlain());

                if (curr == null) {
                    continue bin_iteration_loop;
                }
            }

            TableEntry<K, V> prev;

            /* curr at this point is the bin node */

            for (prev = curr, curr = curr.getNextPlain(); curr != null;) {
                /* If we want to remove, then we should hold prev, as it will be a valid entry to link on */
                if (predicate.test(curr)) {
                    ++removed;
                    this.removeFromSizePlain(1); /* required in case predicate throws an exception */

                    prev.setNextRelease(curr = curr.getNextPlain());
                } else {
                    prev = curr;
                    curr = curr.getNextPlain();
                }
            }
        }

        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(final K key, final V value) {
        Validate.notNull(key, "Null key");
        Validate.notNull(value, "Null value");

        return this.put(key, value, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V putIfAbsent(final K key, final V value) {
        Validate.notNull(key, "Null key");
        Validate.notNull(value, "Null value");

        return this.put(key, value, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Object key, final Object value) {
        Validate.notNull(key, "Null key");
        Validate.notNull(value, "Null value");

        final TableEntry<K, V>[] table = this.getTablePlain();
        final int hash = SWMRHashTable.getHash(key);
        final int index = hash & (table.length - 1);

        final TableEntry<K, V> head = table[index];
        if (head == null) {
            return false;
        }

        if (head.hash == hash && (head.key == key || head.key.equals(key))) {
            final V currVal = head.getValuePlain();

            if (currVal != value && !currVal.equals(value)) {
                return false;
            }

            setAtIndexRelease(table, index, head.getNextPlain());
            this.removeFromSize(1);

            return true;
        }

        for (TableEntry<K, V> curr = head.getNextPlain(), prev = head; curr != null; prev = curr, curr = curr.getNextPlain()) {
            if (curr.hash == hash && (curr.key == key || curr.key.equals(key))) {
                final V currVal = curr.getValuePlain();

                if (currVal != value && !currVal.equals(value)) {
                    return false;
                }

                prev.setNextRelease(curr.getNextPlain());
                this.removeFromSize(1);

                return true;
            }
        }

        return false;
    }

    protected final V remove(final Object key, final int hash) {
        final TableEntry<K, V>[] table = this.getTablePlain();
        final int index = (table.length - 1) & hash;

        final TableEntry<K, V> head = table[index];
        if (head == null) {
            return null;
        }

        if (hash == head.hash && (head.key == key || head.key.equals(key))) {
            setAtIndexRelease(table, index, head.getNextPlain());
            this.removeFromSize(1);

            return head.getValuePlain();
        }

        for (TableEntry<K, V> curr = head.getNextPlain(), prev = head; curr != null; prev = curr, curr = curr.getNextPlain()) {
            if (curr.hash == hash && (key == curr.key || curr.key.equals(key))) {
                prev.setNextRelease(curr.getNextPlain());
                this.removeFromSize(1);

                return curr.getValuePlain();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(final Object key) {
        Validate.notNull(key, "Null key");

        return this.remove(key, SWMRHashTable.getHash(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        Validate.notNull(key, "Null key");
        Validate.notNull(oldValue, "Null oldValue");
        Validate.notNull(newValue, "Null newValue");

        final TableEntry<K, V> entry = this.getEntryForPlain(key);
        if (entry == null) {
            return false;
        }

        final V currValue = entry.getValuePlain();
        if (currValue == oldValue || currValue.equals(oldValue)) {
            entry.setValueRelease(newValue);
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V replace(final K key, final V value) {
        Validate.notNull(key, "Null key");
        Validate.notNull(value, "Null value");

        final TableEntry<K, V> entry = this.getEntryForPlain(key);
        if (entry == null) {
            return null;
        }

        final V prev = entry.getValuePlain();
        entry.setValueRelease(value);
        return prev;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        Validate.notNull(function, "Null function");

        final TableEntry<K, V>[] table = this.getTablePlain();
        for (int i = 0, len = table.length; i < len; ++i) {
            for (TableEntry<K, V> curr = table[i]; curr != null; curr = curr.getNextPlain()) {
                final V value = curr.getValuePlain();

                final V newValue = function.apply(curr.key, value);
                if (newValue == null) {
                    throw new NullPointerException();
                }

                curr.setValueRelease(newValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
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
    @Override
    public void clear() {
        Arrays.fill(this.getTablePlain(), null);
        this.setSizeRelease(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Validate.notNull(key, "Null key");
        Validate.notNull(remappingFunction, "Null remappingFunction");

        final int hash = SWMRHashTable.getHash(key);
        final TableEntry<K, V>[] table = this.getTablePlain();
        final int index = hash & (table.length - 1);

        for (TableEntry<K, V> curr = table[index], prev = null;;prev = curr, curr = curr.getNextPlain()) {
            if (curr == null) {
                final V newVal = remappingFunction.apply(key ,null);

                if (newVal == null) {
                    return null;
                }

                final TableEntry<K, V> insert = new TableEntry<>(hash, key, newVal);
                if (prev == null) {
                    setAtIndexRelease(table, index, insert);
                } else {
                    prev.setNextRelease(insert);
                }

                this.addToSize(1);

                return newVal;
            }

            if (curr.hash == hash && (curr.key == key || curr.key.equals(key))) {
                final V newVal = remappingFunction.apply(key, curr.getValuePlain());

                if (newVal != null) {
                    curr.setValueRelease(newVal);
                    return newVal;
                }

                if (prev == null) {
                    setAtIndexRelease(table, index, curr.getNextPlain());
                } else {
                    prev.setNextRelease(curr.getNextPlain());
                }

                this.removeFromSize(1);

                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Validate.notNull(key, "Null key");
        Validate.notNull(remappingFunction, "Null remappingFunction");

        final int hash = SWMRHashTable.getHash(key);
        final TableEntry<K, V>[] table = this.getTablePlain();
        final int index = hash & (table.length - 1);

        for (TableEntry<K, V> curr = table[index], prev = null; curr != null; prev = curr, curr = curr.getNextPlain()) {
            if (curr.hash != hash || (curr.key != key && !curr.key.equals(key))) {
                continue;
            }

            final V newVal = remappingFunction.apply(key, curr.getValuePlain());
            if (newVal != null) {
                curr.setValueRelease(newVal);
                return newVal;
            }

            if (prev == null) {
                setAtIndexRelease(table, index, curr.getNextPlain());
            } else {
                prev.setNextRelease(curr.getNextPlain());
            }

            this.removeFromSize(1);

            return null;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
        Validate.notNull(key, "Null key");
        Validate.notNull(mappingFunction, "Null mappingFunction");

        final int hash = SWMRHashTable.getHash(key);
        final TableEntry<K, V>[] table = this.getTablePlain();
        final int index = hash & (table.length - 1);

        for (TableEntry<K, V> curr = table[index], prev = null;;prev = curr, curr = curr.getNextPlain()) {
            if (curr != null) {
                if (curr.hash == hash && (curr.key == key || curr.key.equals(key))) {
                    return curr.getValuePlain();
                }
                continue;
            }

            final V newVal = mappingFunction.apply(key);

            if (newVal == null) {
                return null;
            }

            final TableEntry<K, V> insert = new TableEntry<>(hash, key, newVal);
            if (prev == null) {
                setAtIndexRelease(table, index, insert);
            } else {
                prev.setNextRelease(insert);
            }

            this.addToSize(1);

            return newVal;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Validate.notNull(key, "Null key");
        Validate.notNull(value, "Null value");
        Validate.notNull(remappingFunction, "Null remappingFunction");

        final int hash = SWMRHashTable.getHash(key);
        final TableEntry<K, V>[] table = this.getTablePlain();
        final int index = hash & (table.length - 1);

        for (TableEntry<K, V> curr = table[index], prev = null;;prev = curr, curr = curr.getNextPlain()) {
            if (curr == null) {
                final TableEntry<K, V> insert = new TableEntry<>(hash, key, value);
                if (prev == null) {
                    setAtIndexRelease(table, index, insert);
                } else {
                    prev.setNextRelease(insert);
                }

                this.addToSize(1);

                return value;
            }

            if (curr.hash == hash && (curr.key == key || curr.key.equals(key))) {
                final V newVal = remappingFunction.apply(curr.getValuePlain(), value);

                if (newVal != null) {
                    curr.setValueRelease(newVal);
                    return newVal;
                }

                if (prev == null) {
                    setAtIndexRelease(table, index, curr.getNextPlain());
                } else {
                    prev.setNextRelease(curr.getNextPlain());
                }

                this.removeFromSize(1);

                return null;
            }
        }
    }

    protected static final class TableEntry<K, V> implements Entry<K, V> {

        protected static final VarHandle TABLE_ENTRY_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(TableEntry[].class);

        protected final int hash;
        protected final K key;
        protected V value;

        protected TableEntry<K, V> next;

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

        protected final TableEntry<K, V> getNextPlain() {
            //noinspection unchecked
            return (TableEntry<K, V>)NEXT_HANDLE.get(this);
        }

        protected final TableEntry<K, V> getNextOpaque() {
            //noinspection unchecked
            return (TableEntry<K, V>)NEXT_HANDLE.getOpaque(this);
        }

        protected final void setNextPlain(final TableEntry<K, V> next) {
            NEXT_HANDLE.set(this, next);
        }

        protected final void setNextRelease(final TableEntry<K, V> next) {
            NEXT_HANDLE.setRelease(this, next);
        }

        protected TableEntry(final int hash, final K key, final V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.getValueAcquire();
        }

        @Override
        public V setValue(final V value) {
            throw new UnsupportedOperationException();
        }

        protected static int hash(final Object key, final Object value) {
            return key.hashCode() ^ (value == null ? 0 : value.hashCode());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hash(this.key, this.getValueAcquire());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Map.Entry<?, ?> other)) {
                return false;
            }
            final Object otherKey = other.getKey();
            final Object otherValue = other.getValue();

            final K thisKey = this.getKey();
            final V thisVal = this.getValueAcquire();
            return (thisKey == otherKey || thisKey.equals(otherKey)) &&
                (thisVal == otherValue || thisVal.equals(otherValue));
        }
    }


    protected static abstract class TableEntryIterator<K, V, T> implements Iterator<T> {

        protected final TableEntry<K, V>[] table;
        protected final SWMRHashTable<K, V> map;

        /* bin which our current element resides on */
        protected int tableIndex;

        protected TableEntry<K, V> currEntry; /* curr entry, null if no more to iterate or if curr was removed or if we've just init'd */
        protected TableEntry<K, V> nextEntry; /* may not be on the same bin as currEntry */

        protected TableEntryIterator(final TableEntry<K, V>[] table, final SWMRHashTable<K, V> map) {
            this.table = table;
            this.map = map;
            int tableIndex = 0;
            for (int len = table.length; tableIndex < len; ++tableIndex) {
                final TableEntry<K, V> entry = getAtIndexOpaque(table, tableIndex);
                if (entry != null) {
                    this.nextEntry = entry;
                    this.tableIndex = tableIndex + 1;
                    return;
                }
            }
            this.tableIndex = tableIndex;
        }

        @Override
        public boolean hasNext() {
            return this.nextEntry != null;
        }

        protected final TableEntry<K, V> advanceEntry() {
            final TableEntry<K, V>[] table = this.table;
            final int tableLength = table.length;
            int tableIndex = this.tableIndex;
            final TableEntry<K, V> curr = this.nextEntry;
            if (curr == null) {
                return null;
            }

            this.currEntry = curr;

            // set up nextEntry

            // find next in chain
            TableEntry<K, V> next = curr.getNextOpaque();

            if (next != null) {
                this.nextEntry = next;
                return curr;
            }

            // nothing in chain, so find next available bin
            for (;tableIndex < tableLength; ++tableIndex) {
                next = getAtIndexOpaque(table, tableIndex);
                if (next != null) {
                    this.nextEntry = next;
                    this.tableIndex = tableIndex + 1;
                    return curr;
                }
            }

            this.nextEntry = null;
            this.tableIndex = tableIndex;
            return curr;
        }

        @Override
        public void remove() {
            final TableEntry<K, V> curr = this.currEntry;
            if (curr == null) {
                throw new IllegalStateException();
            }

            this.map.remove(curr.key, curr.hash);

            this.currEntry = null;
        }
    }

    protected static final class ValueIterator<K, V> extends TableEntryIterator<K, V, V> {

        protected ValueIterator(final TableEntry<K, V>[] table, final SWMRHashTable<K, V> map) {
            super(table, map);
        }

        @Override
        public V next() {
            final TableEntry<K, V> entry = this.advanceEntry();

            if (entry == null) {
                throw new NoSuchElementException();
            }

            return entry.getValueAcquire();
        }
    }

    protected static final class KeyIterator<K, V> extends TableEntryIterator<K, V, K> {

        protected KeyIterator(final TableEntry<K, V>[] table, final SWMRHashTable<K, V> map) {
            super(table, map);
        }

        @Override
        public K next() {
            final TableEntry<K, V> curr = this.advanceEntry();

            if (curr == null) {
                throw new NoSuchElementException();
            }

            return curr.key;
        }
    }

    protected static final class EntryIterator<K, V> extends TableEntryIterator<K, V, Entry<K, V>> {

        protected EntryIterator(final TableEntry<K, V>[] table, final SWMRHashTable<K, V> map) {
            super(table, map);
        }

        @Override
        public Entry<K, V> next() {
            final TableEntry<K, V> curr = this.advanceEntry();

            if (curr == null) {
                throw new NoSuchElementException();
            }

            return curr;
        }
    }

    protected static abstract class ViewCollection<K, V, T> implements Collection<T> {

        protected final SWMRHashTable<K, V> map;

        protected ViewCollection(final SWMRHashTable<K, V> map) {
            this.map = map;
        }

        @Override
        public boolean add(final T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(final Collection<? extends T> collections) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(final Collection<?> collection) {
            Validate.notNull(collection, "Null collection");

            boolean modified = false;
            for (final Object element : collection) {
                modified |= this.remove(element);
            }
            return modified;
        }

        @Override
        public int size() {
            return this.map.size();
        }

        @Override
        public boolean isEmpty() {
            return this.size() == 0;
        }

        @Override
        public void clear() {
            this.map.clear();
        }

        @Override
        public boolean containsAll(final Collection<?> collection) {
            Validate.notNull(collection, "Null collection");

            for (final Object element : collection) {
                if (!this.contains(element)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public Object[] toArray() {
            final List<T> list = new ArrayList<>(this.size());

            this.forEach(list::add);

            return list.toArray();
        }

        @Override
        public <E> E[] toArray(final E[] array) {
            final List<T> list = new ArrayList<>(this.size());

            this.forEach(list::add);

            return list.toArray(array);
        }

        @Override
        public <E> E[] toArray(final IntFunction<E[]> generator) {
            final List<T> list = new ArrayList<>(this.size());

            this.forEach(list::add);

            return list.toArray(generator);
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (final T element : this) {
                hash += element == null ? 0 : element.hashCode();
            }
            return hash;
        }

        @Override
        public Spliterator<T> spliterator() { // TODO implement
            return Spliterators.spliterator(this, Spliterator.NONNULL);
        }
    }

    protected static abstract class ViewSet<K, V, T> extends ViewCollection<K, V, T> implements Set<T> {

        protected ViewSet(final SWMRHashTable<K, V> map) {
            super(map);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Set)) {
                return false;
            }

            final Set<?> other = (Set<?>)obj;
            if (other.size() != this.size()) {
                return false;
            }

            return this.containsAll(other);
        }
    }

    protected static final class EntrySet<K, V> extends ViewSet<K, V, Entry<K, V>> implements Set<Entry<K, V>> {

        protected EntrySet(final SWMRHashTable<K, V> map) {
            super(map);
        }

        @Override
        public boolean remove(final Object object) {
            if (!(object instanceof Map.Entry<?, ?> entry)) {
                return false;
            }

            final Object key;
            final Object value;

            try {
                key = entry.getKey();
                value = entry.getValue();
            } catch (final IllegalStateException ex) {
                return false;
            }

            return this.map.remove(key, value);
        }

        @Override
        public boolean removeIf(final Predicate<? super Entry<K, V>> filter) {
            Validate.notNull(filter, "Null filter");

            return this.map.removeEntryIf(filter) != 0;
        }

        @Override
        public boolean retainAll(final Collection<?> collection) {
            Validate.notNull(collection, "Null collection");

            return this.map.removeEntryIf((final Entry<K, V> entry) -> {
                return !collection.contains(entry);
            }) != 0;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator<>(this.map.getTableAcquire(), this.map);
        }

        @Override
        public void forEach(final Consumer<? super Entry<K, V>> action) {
            this.map.forEach(action);
        }

        @Override
        public boolean contains(final Object object) {
            if (!(object instanceof Map.Entry<?, ?> entry)) {
                return false;
            }

            final Object key;
            final Object value;

            try {
                key = entry.getKey();
                value = entry.getValue();
            } catch (final IllegalStateException ex) {
                return false;
            }

            return this.map.contains(key, value);
        }

        @Override
        public String toString() {
            return CollectionUtil.toString(this, "SWMRHashTableEntrySet");
        }
    }

    protected static final class KeySet<K, V> extends ViewSet<K, V, K> {

        protected KeySet(final SWMRHashTable<K, V> map) {
            super(map);
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator<>(this.map.getTableAcquire(), this.map);
        }

        @Override
        public void forEach(final Consumer<? super K> action) {
            Validate.notNull(action, "Null action");

            this.map.forEachKey(action);
        }

        @Override
        public boolean contains(final Object key) {
            Validate.notNull(key, "Null key");

            return this.map.containsKey(key);
        }

        @Override
        public boolean remove(final Object key) {
            Validate.notNull(key, "Null key");

            return this.map.remove(key) != null;
        }

        @Override
        public boolean retainAll(final Collection<?> collection) {
            Validate.notNull(collection, "Null collection");

            return this.map.removeIf((final K key, final V value) -> {
                return !collection.contains(key);
            }) != 0;
        }

        @Override
        public boolean removeIf(final Predicate<? super K> filter) {
            Validate.notNull(filter, "Null filter");

            return this.map.removeIf((final K key, final V value) -> {
                return filter.test(key);
            }) != 0;
        }

        @Override
        public String toString() {
            return CollectionUtil.toString(this, "SWMRHashTableKeySet");
        }
    }

    protected static final class ValueCollection<K, V> extends ViewSet<K, V, V> implements Collection<V> {

        protected ValueCollection(final SWMRHashTable<K, V> map) {
            super(map);
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator<>(this.map.getTableAcquire(), this.map);
        }

        @Override
        public void forEach(final Consumer<? super V> action) {
            Validate.notNull(action, "Null action");

            this.map.forEachValue(action);
        }

        @Override
        public boolean contains(final Object object) {
            Validate.notNull(object, "Null object");

            return this.map.containsValue(object);
        }

        @Override
        public boolean remove(final Object object) {
            Validate.notNull(object, "Null object");

            final Iterator<V> itr = this.iterator();
            while (itr.hasNext()) {
                final V val = itr.next();
                if (val == object || val.equals(object)) {
                    itr.remove();
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean removeIf(final Predicate<? super V> filter) {
            Validate.notNull(filter, "Null filter");

            return this.map.removeIf((final K key, final V value) -> {
                return filter.test(value);
            }) != 0;
        }

        @Override
        public boolean retainAll(final Collection<?> collection) {
            Validate.notNull(collection, "Null collection");

            return this.map.removeIf((final K key, final V value) -> {
                return !collection.contains(value);
            }) != 0;
        }

        @Override
        public String toString() {
            return CollectionUtil.toString(this, "SWMRHashTableValues");
        }
    }
}
