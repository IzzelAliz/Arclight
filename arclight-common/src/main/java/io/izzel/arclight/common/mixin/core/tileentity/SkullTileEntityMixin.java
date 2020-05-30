package io.izzel.arclight.common.mixin.core.tileentity;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;
import com.mysql.jdbc.StringUtils;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.util.ArclightHeadLoader;
import net.minecraft.tileentity.SkullTileEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Mixin(SkullTileEntity.class)
public abstract class SkullTileEntityMixin extends TileEntityMixin {

    // @formatter:off
    @Shadow public GameProfile playerProfile;
    // @formatter:on

    private static ExecutorService executor = Executors.newFixedThreadPool(3,
        new ThreadFactoryBuilder()
            .setNameFormat("Head Conversion Thread - %1$d")
            .build()
    );

    private static LoadingCache<String, GameProfile> skinCache = CacheBuilder.newBuilder().maximumSize(5000L).expireAfterAccess(60L, TimeUnit.MINUTES).build(new ArclightHeadLoader());

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void updatePlayerProfile() {
        GameProfile profile = this.playerProfile;
        b(profile, input -> {
            playerProfile = input;
            markDirty();
            return false;
        }, false);
    }

    @SuppressWarnings({"UnusedReturnValue", "ConstantConditions"})
    private static Future<GameProfile> b(GameProfile gameprofile, Predicate<GameProfile> callback, boolean sync) {
        if (gameprofile != null && !StringUtils.isNullOrEmpty(gameprofile.getName())) {
            if (gameprofile.isComplete() && gameprofile.getProperties().containsKey("textures")) {
                callback.apply(gameprofile);
            } else if (Bukkit.getServer() == null || ((CraftServer) Bukkit.getServer()).getServer() == null) {
                callback.apply(gameprofile);
            } else {
                GameProfile profile = skinCache.getIfPresent(gameprofile.getName().toLowerCase(Locale.ROOT));
                if (profile != null && Iterables.getFirst((profile.getProperties()).get("textures"), null) != null) {
                    callback.apply(profile);
                    return Futures.immediateFuture(profile);
                }
                Callable<GameProfile> callable = () -> {
                    GameProfile profile1 = skinCache.getUnchecked(gameprofile.getName().toLowerCase(Locale.ROOT));
                    ((MinecraftServerBridge) ((CraftServer) Bukkit.getServer()).getServer()).bridge$queuedProcess(() -> {
                        if (profile1 == null) {
                            callback.apply(gameprofile);
                        } else {
                            callback.apply(profile1);
                        }
                    });
                    return profile1;
                };
                if (sync) {
                    try {
                        return Futures.immediateFuture(callable.call());
                    } catch (Exception ex) {
                        Throwables.throwIfUnchecked(ex);
                        throw new RuntimeException(ex);
                    }
                }
                return executor.submit(callable);
            }
        } else {
            callback.apply(gameprofile);
        }
        return Futures.immediateFuture(gameprofile);
    }
}
