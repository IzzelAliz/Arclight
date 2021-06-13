package io.izzel.arclight.common.mixin.api;

import io.izzel.arclight.api.Arclight;
import io.izzel.arclight.common.mod.util.PluginEventHandler;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Arclight.class, remap = false)
public class Arclight_ForgeEventMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void registerForgeEvent(Plugin plugin, IEventBus bus, Object target) throws Throwable {
        if (bus instanceof EventBus) {
            PluginEventHandler.register(plugin, (EventBus) bus, target);
        } else {
            bus.register(target);
        }
    }
}
