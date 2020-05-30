package io.izzel.arclight.common.bridge.server.management;

import java.util.Collection;

public interface UserListBridge<V> {

    Collection<V> bridge$getValues();
}
