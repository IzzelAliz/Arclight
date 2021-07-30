package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.CustomServerBossInfoBridge;
import net.minecraft.server.bossevents.CustomBossEvent;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.craftbukkit.v.boss.CraftKeyedBossbar;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CustomBossEvent.class)
public class CustomServerBossInfoMixin implements CustomServerBossInfoBridge {

    private KeyedBossBar bossBar;

    public KeyedBossBar getBukkitEntity() {
        if (bossBar == null) {
            bossBar = new CraftKeyedBossbar((CustomBossEvent) (Object) this);
        }
        return bossBar;
    }

    @Override
    public KeyedBossBar bridge$getBukkitEntity() {
        return getBukkitEntity();
    }
}
