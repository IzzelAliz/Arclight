package io.izzel.arclight.mixin.core.server;

import io.izzel.arclight.bridge.server.CustomServerBossInfoBridge;
import net.minecraft.server.CustomServerBossInfo;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.craftbukkit.v1_14_R1.boss.CraftKeyedBossbar;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CustomServerBossInfo.class)
public class CustomServerBossInfoMixin implements CustomServerBossInfoBridge {

    private KeyedBossBar bossBar;

    public KeyedBossBar getBukkitEntity() {
        if (bossBar == null) {
            bossBar = new CraftKeyedBossbar((CustomServerBossInfo) (Object) this);
        }
        return bossBar;
    }

    @Override
    public KeyedBossBar bridge$getBukkitEntity() {
        return getBukkitEntity();
    }
}
