package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.server.ArclightServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import org.bukkit.GameRule;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.chunks.CraftChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
// import org.spongepowered.asm.mixin.Final;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.Overwrite;
// import org.spongepowered.asm.mixin.Shadow;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.Redirect;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(CraftWorld.class)
public abstract class CraftWorldMixin {

    private static final java.util.concurrent.Executor MAIN_EXECUTOR = (run) -> {
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.getMinecraftServer().execute(run);
        } else {
            run.run();
        }
    };

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    private static final TicketType<Unit> PLUGIN = Unsafe.getStatic(TicketType.class, "PLUGIN");

    /**
     * @author MemencioPerez
     * @reason Implement Paper patch "Add Plugin Tickets to API Chunk Methods"
     */
    @Overwrite(remap = false)
    public Chunk getChunkAt(int x, int z) {
        // Paper start - add ticket to hold chunk for a little while longer if plugin accesses it
        net.minecraft.world.level.chunk.LevelChunk chunk = ((ServerChunkProviderBridge) this.world.getChunkSource()).bridge$getChunkAtIfLoadedImmediately(x, z);
        if (chunk == null) {
            this.addTicket(x, z);
            chunk = this.world.getChunkSource().getChunk(x, z, true);
        }
        // Paper end
        return new CraftChunk(chunk);
    }

    // Paper start
    @Unique
    private void addTicket(int x, int z) {
        MAIN_EXECUTOR.execute(() -> this.world.getChunkSource().addRegionTicket(PLUGIN, new ChunkPos(x, z), 0, Unit.INSTANCE)); // Paper
    }
    // Paper end

    @ModifyArg(method = "unloadChunkRequest", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;removeRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"), index = 2)
    private int arclight$changeRemoveDistance(int distance) {
        return 0; // Paper
    }

    @ModifyArg(method = "loadChunk(IIZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"), index = 2)
    private int arclight$changeAddDistance(int distance) {
        return 0; // Paper
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public File getWorldFolder() {
        return ((ServerLevelBridge) this.world).bridge$getConvertable().getDimensionPath(this.world.dimension()).toFile();
    }

    @Inject(method = "convert", cancellable = true, at = @At("HEAD"), remap = false)
    private<T> void arclight$fallbackConvert(GameRule<T> rule, GameRules.Value<?> value, CallbackInfoReturnable<T> cir) {
        if (rule.getType() == String.class) {
            cir.setReturnValue(rule.getType().cast(value.serialize()));
        }
    }

    @Redirect(method = "getGameRuleValue(Ljava/lang/String;)Ljava/lang/String;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules$Value;toString()Ljava/lang/String;"))
    private String arclight$useSerialize(GameRules.Value<?> instance) {
        return instance.serialize();
    }
}
