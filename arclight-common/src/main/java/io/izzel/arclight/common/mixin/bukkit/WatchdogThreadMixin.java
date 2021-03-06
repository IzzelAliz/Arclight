package io.izzel.arclight.common.mixin.bukkit;

import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = WatchdogThread.class, remap = false)
public class WatchdogThreadMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void doStart(int timeoutTime, boolean restart) {
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void tick() {
    }
}
