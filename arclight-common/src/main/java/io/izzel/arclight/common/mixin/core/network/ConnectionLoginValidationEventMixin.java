package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.ConnectionLoginValidationBridge;
import io.izzel.arclight.common.mod.util.ConnectionLoginValidation;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionLoginValidationEventMixin implements ConnectionLoginValidationBridge {

    @Unique private final ConnectionLoginValidation.State arclight$loginValidationState = new ConnectionLoginValidation.State();

    @Override
    public ConnectionLoginValidation.State bridge$getLoginValidationState() {
        return this.arclight$loginValidationState;
    }
}
