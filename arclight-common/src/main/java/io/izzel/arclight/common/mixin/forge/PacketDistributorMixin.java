package io.izzel.arclight.common.mixin.forge;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraftforge.fml.network.PacketDistributor;
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
    private Consumer<IPacket<?>> playerConsumer(Supplier<ServerPlayerEntity> entityPlayerMPSupplier) {
        return p -> {
            ServerPlayerEntity entity = entityPlayerMPSupplier.get();
            if (entity.connection != null && entity.connection.netManager != null) {
                entity.connection.netManager.sendPacket(p);
            }
        };
    }
}
