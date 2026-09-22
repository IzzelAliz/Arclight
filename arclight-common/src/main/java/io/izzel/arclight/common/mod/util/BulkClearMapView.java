package io.izzel.arclight.common.mod.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A public, ordinary-Java filtered map view for bulk removals. It deliberately
 * lives outside a mixin package: Mixins only select the call site; this class
 * owns the collection contract used by the selected call sites.
 */
public final class BulkClearMapView<K, V> extends AbstractMap<K, V> {

    private final Map<K, V> delegate;
    private final Map<K, V> accepted;

    public BulkClearMapView(Map<K, V> delegate, Map<K, V> accepted) {
        this.delegate = delegate;
        this.accepted = accepted;
    }

    /**
     * Copies keys and values into immutable entries before event dispatch can
     * mutate the backing map. LinkedHashSet preserves the source traversal
     * order without retaining the source map's live entry objects.
     */
    public static <K, V> Set<Map.Entry<K, V>> immutableEntrySnapshot(Set<Map.Entry<K, V>> entries) {
        Set<Map.Entry<K, V>> snapshot = new LinkedHashSet<>(entries.size());
        for (Map.Entry<K, V> entry : entries) {
            snapshot.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableSet(snapshot);
    }

    /**
     * Freezes only candidate keys whose values still have the same identity in
     * activeEffects. This makes a later key-only removal skip a value replaced
     * by a synchronous event listener.
     */
    public static <K, V> Set<K> matchingKeys(Set<K> keys, Map<K, V> candidates, Map<K, V> activeEffects) {
        Set<K> matching = new LinkedHashSet<>(keys.size());
        for (K key : keys) {
            if (activeEffects.get(key) == candidates.get(key)) {
                matching.add(key);
            }
        }
        return Collections.unmodifiableSet(matching);
    }

    @Override
    public V put(K key, V value) {
        return this.delegate.put(key, value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        List<Entry<K, V>> visible = new ArrayList<>();
        for (Entry<K, V> entry : this.delegate.entrySet()) {
            if (this.accepted.get(entry.getKey()) == entry.getValue()) {
                visible.add(new AbstractMap.SimpleImmutableEntry<>(entry));
            }
        }
        return new AbstractSet<>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                Iterator<Entry<K, V>> iterator = visible.iterator();
                return new Iterator<>() {
                    private Entry<K, V> current;

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<K, V> next() {
                        return current = iterator.next();
                    }

                    @Override
                    public void remove() {
                        delegate.remove(current.getKey(), current.getValue());
                    }
                };
            }

            @Override
            public int size() {
                return visible.size();
            }
        };
    }
}
