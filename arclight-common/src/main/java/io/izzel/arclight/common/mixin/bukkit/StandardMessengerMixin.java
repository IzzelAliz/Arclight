package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.plugin.messaging.Messenger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.bukkit.plugin.messaging.StandardMessenger;

@Mixin(value=StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin implements Messenger {

    @ModifyConstant(
            method = "validateAndCorrectChannel",
            constant = @Constant(intValue = Messenger.MAX_CHANNEL_SIZE)
    )
    private static int modifyMaxChannelSize(int original) {
        return 256;
    }
}