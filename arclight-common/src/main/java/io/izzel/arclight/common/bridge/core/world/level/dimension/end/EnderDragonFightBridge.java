package io.izzel.arclight.common.bridge.core.world.level.dimension.end;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import java.util.List;

/** Bukkit's boolean result is supplied by the caller bridge, not a return-type overload of NMS. */
public interface EnderDragonFightBridge {

    boolean bridge$tryRespawn();

    boolean bridge$respawnDragon(List<EndCrystal> crystals);

    void bridge$setPreviouslyKilled(boolean killed);
}
