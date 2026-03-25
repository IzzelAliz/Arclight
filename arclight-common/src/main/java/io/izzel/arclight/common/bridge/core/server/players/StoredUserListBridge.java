package io.izzel.arclight.common.bridge.core.server.players;

import java.util.Collection;

public interface StoredUserListBridge<V> {

    Collection<V> bridge$getValues();
}
