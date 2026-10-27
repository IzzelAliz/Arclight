package io.izzel.arclight.forge.mixin.forge;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Consumer;

@Mixin(PacketDistributor.class)
public class PacketDistributorMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    private Consumer<Packet<?>> playerConsumer(ServerPlayer entity) {
        return p -> {
            if (entity.connection != null && entity.connection.isAcceptingMessages()) {
                entity.connection.send(p);
            }
        };
    }
}
