package io.izzel.arclight.common.mixin.core.world.level.storage;

import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataMixin implements PrimaryLevelDataBridge {

    // @formatter:off
    @Shadow public abstract String getLevelName();
    @Shadow public abstract boolean isDifficultyLocked();
    @Shadow public LevelSettings settings;
    @Shadow @Final private Lifecycle worldGenSettingsLifecycle;
    // @formatter:on

    public ServerLevel world;
    public Registry<LevelStem> customDimensions;

    @Inject(method = "setDifficulty", at = @At("RETURN"))
    private void arclight$sendDiffChange(Difficulty newDifficulty, CallbackInfo ci) {
        ClientboundChangeDifficultyPacket packet = new ClientboundChangeDifficultyPacket(newDifficulty, this.isDifficultyLocked());
        for (ServerPlayer player : this.world.getServer().getPlayerList().getPlayers()) {
            if (arclight$ownsDifficulty(player.level().getLevelData())) {
                player.connection.send(packet);
            }
        }
    }

    private boolean arclight$ownsDifficulty(LevelData data) {
        if (data == (Object) this) {
            return true;
        }
        if (data instanceof DerivedLevelDataBridge bridge) {
            return bridge.bridge$getWorldData() == (Object) this;
        }
        return false;
    }

    @Override
    public void bridge$setWorld(ServerLevel world) {
        setWorld(world);
    }

    public void setWorld(ServerLevel world) {
        if (this.world == null) {
            this.world = world;
        }
    }

    @Override
    public ServerLevel bridge$getWorld() {
        return world;
    }

    public void checkName(String name) {
        if (!this.settings.levelName.equals(name)) {
            this.settings.levelName = name;
        }
    }

    @Override
    public void arclight$checkName(String name) {
        checkName(name);
    }

    @Override
    public void arclight$offerCustomDimensions(Registry<LevelStem> registry) {
        this.customDimensions = registry;
    }

    @Override
    public LevelSettings bridge$getWorldSettings() {
        return this.settings;
    }

    @Override
    public Lifecycle bridge$getLifecycle() {
        return this.worldGenSettingsLifecycle;
    }
}
