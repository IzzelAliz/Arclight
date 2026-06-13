package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = CraftPlayer.class, remap = false)
public abstract class CraftPlayerMixin extends CraftEntityMixin {

    @Shadow @Final @Mutable private Set<String> channels;

    @Shadow public abstract ServerPlayer getHandle();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void arclight$useFastSet(CraftServer server, ServerPlayer entity, CallbackInfo ci) {
        channels = new ObjectOpenHashSet<>();
    }

    @ModifyConstant(method = "addChannel", constant = @Constant(intValue = 128))
    private int arclight$modifyMaxChannel(int constant) {
        return 2048;
    }
    
    /**
     * @author InitAuther97
     * @reason Use PSI and enhanced check.
     */
    @Overwrite
    private void sendCustomPayload(Identifier location, byte[] data) {
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
                Identifier location = Identifier.tryParse(StandardMessenger.validateAndCorrectChannel(channel));
                var craft = (CraftPlayer)(Object) this;
                messenger.arclight$sendCustomPayload(source, craft, location, data);
            }
        }
    }

}
