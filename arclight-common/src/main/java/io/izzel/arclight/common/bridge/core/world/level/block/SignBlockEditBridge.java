package io.izzel.arclight.common.bridge.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.bukkit.event.player.PlayerSignOpenEvent;

public interface SignBlockEditBridge {

    void bridge$openTextEdit(Player player, SignBlockEntity sign, boolean front,
                            PlayerSignOpenEvent.Cause cause, Operation<Void> original);
}
