package io.izzel.arclight.common.mod.server.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class ArclightFakePlayer extends CraftPlayer {

    public ArclightFakePlayer(CraftServer server, ServerPlayer entity) {
        super(server, entity);
    }

    @Override
    public boolean isOp() {
        GameProfile profile = this.getHandle().getGameProfile();
        return profile != null && profile.id() != null && super.isOp();
    }

    @Override
    public void setOp(boolean value) {
    }
}
