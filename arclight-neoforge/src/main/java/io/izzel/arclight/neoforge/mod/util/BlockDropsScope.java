package io.izzel.arclight.neoforge.mod.util;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

public final class BlockDropsScope {

    private final ServerLevel level;
    private final ArclightCaptures.BlockBreakEventContext context;
    private final List<ItemEntity> entities = new ArrayList<>();

    public BlockDropsScope(ServerLevel level, ArclightCaptures.BlockBreakEventContext context) {
        this.level = level;
        this.context = context;
    }

    public ServerLevel level() {
        return level;
    }

    public ArclightCaptures.BlockBreakEventContext context() {
        return context;
    }

    public void capture(List<ItemEntity> drops) {
        entities.addAll(drops);
    }

    public boolean consume(ItemEntity entity) {
        for (int i = 0; i < entities.size(); i++) {
            if (entities.get(i) == entity) {
                entities.remove(i);
                return true;
            }
        }
        return false;
    }
}
