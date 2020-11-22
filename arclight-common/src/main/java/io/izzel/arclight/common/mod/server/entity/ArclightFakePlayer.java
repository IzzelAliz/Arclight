package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;

public class ArclightFakePlayer extends CraftPlayer {

    public ArclightFakePlayer(CraftServer server, ServerPlayerEntity entity) {
        super(server, entity);
    }

    @Override
    public boolean isOp() {
        return this.getHandle().getGameProfile().getId() != null && super.isOp();
    }

    @Override
    public void setOp(boolean value) {
    }
}
