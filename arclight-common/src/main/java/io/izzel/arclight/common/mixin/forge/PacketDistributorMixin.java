package io.izzel.arclight.common.mixin.forge;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(PacketDistributor.class)
public class PacketDistributorMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    private Consumer<Packet<?>> playerConsumer(Supplier<ServerPlayer> entityPlayerMPSupplier) {
        return p -> {
            ServerPlayer entity = entityPlayerMPSupplier.get();
            if (entity.connection != null && entity.connection.connection != null) {
                entity.connection.connection.send(p);
            }
        };
    }
}
