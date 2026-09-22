package io.izzel.arclight.common.mod.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/** Full snapshot view for loader veto restoration; never overwrites a replacement. */
public final class EffectRestoreMap<K, V> extends AbstractMap<K, V> {
    private final Map<K, V> delegate;

    public EffectRestoreMap(Map<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public V get(Object key) {
        return this.delegate.get(key);
    }

    @Override
    public V put(K key, V snapshot) {
        V current = this.delegate.get(key);
        if (current == null || current == snapshot) {
            return this.delegate.put(key, snapshot);
        }
        return current;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (this.delegate.get(key) != value) {
            return false;
        }
        return this.delegate.remove(key, value);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return BulkClearMapView.immutableEntrySnapshot(this.delegate.entrySet());
    }
}
