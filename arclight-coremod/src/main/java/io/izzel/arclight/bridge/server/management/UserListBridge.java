package io.izzel.arclight.bridge.server.management;

import java.util.Collection;

public interface UserListBridge<V> {

    Collection<V> bridge$getValues();
}
