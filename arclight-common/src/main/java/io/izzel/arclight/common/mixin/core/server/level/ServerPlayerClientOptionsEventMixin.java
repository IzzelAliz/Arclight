package io.izzel.arclight.common.mixin.core.server.level;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.PaperSkinParts;
import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.inventory.MainHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerClientOptionsEventMixin {

    @Unique private boolean arclight$clientOptionsInitialized;

    @Decorate(method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)V",
        inject = true, at = @At("RETURN"), require = 1, allow = 1)
    private void arclight$finishInitialClientOptions() {
        // Native constructors also call updateOptions. Paper initializes login,
        // respawn and reconfiguration players without settings events.
        this.arclight$clientOptionsInitialized = true;
    }

    @WrapMethod(method = "updateOptions(Lnet/minecraft/server/level/ClientInformation;)V",
        require = 1, expect = 1, allow = 1)
    private void arclight$clientOptionsNotification(ClientInformation information, Operation<Void> original) {
        if (this.arclight$clientOptionsInitialized) {
            Map<ClientOption<?>, Object> options = new IdentityHashMap<>();
            options.put(ClientOption.LOCALE, information.language());
            options.put(ClientOption.VIEW_DISTANCE, information.viewDistance());
            options.put(ClientOption.CHAT_VISIBILITY, ClientOption.ChatVisibility.valueOf(information.chatVisibility().name()));
            options.put(ClientOption.CHAT_COLORS_ENABLED, information.chatColors());
            options.put(ClientOption.SKIN_PARTS, new PaperSkinParts(information.modelCustomisation()));
            options.put(ClientOption.MAIN_HAND, information.mainHand() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT);
            options.put(ClientOption.TEXT_FILTERING_ENABLED, information.textFilteringEnabled());
            options.put(ClientOption.ALLOW_SERVER_LISTINGS, information.allowsListing());
            options.put(ClientOption.PARTICLE_VISIBILITY, ClientOption.ParticleVisibility.valueOf(information.particleStatus().name()));
            new PlayerClientOptionsChangeEvent(((ServerPlayerBridge) this).bridge$getBukkitEntity(), options).callEvent();

            // Paper reads each legacy predicate after the preceding callbacks.
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (player.clientInformation().mainHand() != information.mainHand()) {
                new PlayerChangedMainHandEvent(((ServerPlayerBridge) this).bridge$getBukkitEntity(),
                    information.mainHand() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT).callEvent();
            }
            String language = player.clientInformation().language();
            if (language == null || !language.equals(information.language())) {
                new PlayerLocaleChangeEvent(((ServerPlayerBridge) this).bridge$getBukkitEntity(), information.language()).callEvent();
            }
        }
        // Initialization skips every settings event, never the native writes.
        original.call(information);
    }
}
