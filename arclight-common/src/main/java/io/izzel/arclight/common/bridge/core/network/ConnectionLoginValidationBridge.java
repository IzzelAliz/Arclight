package io.izzel.arclight.common.bridge.core.network;

import io.izzel.arclight.common.mod.util.ConnectionLoginValidation;

public interface ConnectionLoginValidationBridge {

    ConnectionLoginValidation.State bridge$getLoginValidationState();
}
