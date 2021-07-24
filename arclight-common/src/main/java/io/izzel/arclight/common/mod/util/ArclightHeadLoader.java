package io.izzel.arclight.common.mod.util;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.Iterables;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public class ArclightHeadLoader extends CacheLoader<String, GameProfile> {

    @Override
    public GameProfile load(String key) {
        GameProfile[] profiles = {null};
        ProfileLookupCallback gameProfileLookup = new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile gp) {
                profiles[0] = gp;
            }

            @Override
            public void onProfileLookupFailed(GameProfile gp, Exception excptn) {
                profiles[0] = gp;
            }
        };
        ((CraftServer) Bukkit.getServer()).getServer().getProfileRepository().findProfilesByNames(new String[]{key}, Agent.MINECRAFT, gameProfileLookup);
        GameProfile profile = profiles[0];
        if (profile == null) {
            UUID uuid = Player.createPlayerUUID(new GameProfile(null, key));
            profile = new GameProfile(uuid, key);
            gameProfileLookup.onProfileLookupSucceeded(profile);
        } else {
            Property property = Iterables.getFirst((profile.getProperties()).get("textures"), null);
            if (property == null) {
                profile = SkullBlockEntity.sessionService.fillProfileProperties(profile, true);
            }
        }
        return profile;
    }
}
