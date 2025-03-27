package io.izzel.arclight.neoforge.mixin.neoforge;

import io.izzel.arclight.neoforge.mod.plugin.messaging.ArclightNfMessaging;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = NetworkComponentNegotiator.class, remap = false)
public abstract class NetworkComponentNegotiatorMixin {
    @SuppressWarnings("StringEquality")
    @Redirect(
            method = "validateComponent",
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            ordinal = 1,
                            target = "Lnet/neoforged/neoforge/network/negotiation/NegotiableNetworkComponent;version()Ljava/lang/String;"
                    ),
                    to = @At(
                            value = "INVOKE",
                            ordinal = 2,
                            target = "Lnet/neoforged/neoforge/network/negotiation/NegotiableNetworkComponent;version()Ljava/lang/String;"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"
            )
    )
    private static boolean arclight$bypassValidation(String instance, Object o) {
        if (instance == ArclightNfMessaging.ARCLIGHT_CUSTOM_CHANNEL_VERSION || o == ArclightNfMessaging.ARCLIGHT_CUSTOM_CHANNEL_VERSION) {
            return true;
        }
        return instance.equals(o);
    }
}
