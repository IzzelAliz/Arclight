package io.izzel.arclight.common.mixin.core.world.entity.raid;

import io.izzel.arclight.common.bridge.core.world.entity.raid.RaidBridge;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(Raid.class)
public class RaidMixin implements RaidBridge {

    // @formatter:off
    @Shadow @Final private Map<Integer, Set<Raider>> groupRaiderMap;
    // @formatter:on

    public Collection<Raider> getRaiders() {
        HashSet<Raider> set = new HashSet<>();
        for (Set<Raider> entities : this.groupRaiderMap.values()) {
            set.addAll(entities);
        }
        return set;
    }

    @Override
    public Collection<Raider> bridge$getRaiders() {
        return getRaiders();
    }
}
