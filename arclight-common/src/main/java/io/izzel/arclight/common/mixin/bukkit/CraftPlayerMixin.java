package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import java.util.Set;

@Mixin(value = CraftPlayer.class, remap = false)
public abstract class CraftPlayerMixin extends CraftEntityMixin {

    @Shadow @Final private Set<String> channels;

    @Shadow public abstract ServerPlayer getHandle();

    @ModifyConstant(method = "addChannel", constant = @Constant(intValue = 128))
    private int arclight$modifyMaxChannel(int constant) {
        return 1024;
    }
    
    /**
     * @author InitAuther97
     * @reason Use PSI and enhanced check.
     */
    @Overwrite
    private void sendCustomPayload(ResourceLocation location, byte[] data) {
        var messenger = (MessengerBridge) server.getMessenger();
        var craft = (CraftPlayer)(Object) this;
        messenger.arclight$sendCustomPayload(null, craft, location, data);
    }

    /**
     * @author InitAuther97
     * @reason Use PSI and enhanced check.
     */
    @Overwrite
    public void sendPluginMessage(Plugin source, String channel, byte[] data) {
        var messenger = (MessengerBridge) server.getMessenger();
        StandardMessenger.validatePluginMessage(server.getMessenger(), source, channel, data);
        if (this.getHandle().connection != null) {
            if (this.channels.contains(channel)) {
                ResourceLocation location = ResourceLocation.tryParse(StandardMessenger.validateAndCorrectChannel(channel));
                var craft = (CraftPlayer)(Object) this;
                messenger.arclight$sendCustomPayload(source, craft, location, data);
            }
        }
    }

}
