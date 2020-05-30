package io.izzel.arclight.common.bridge.entity.passive;

import java.util.UUID;

public interface FoxEntityBridge extends AnimalEntityBridge {

    void bridge$addTrustedUUID(UUID uuidIn);
}
