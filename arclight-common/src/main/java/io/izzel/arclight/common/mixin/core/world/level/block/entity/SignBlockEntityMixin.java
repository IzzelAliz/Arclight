package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.tileentity.SignTileEntityBridge;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftSign;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntityMixin implements SignTileEntityBridge, CommandSource, ICommandSourceBridge {

    // @formatter:off
    @Shadow public abstract ClientboundBlockEntityDataPacket getUpdatePacket();
    @Shadow private static CommandSourceStack createCommandSourceStack(@Nullable Player p_279428_, Level p_279359_, BlockPos p_279430_) { return null; }
    // @formatter:on

    @Inject(method = "updateSignText", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private void arclight$updatePacket(Player player, boolean p_278103_, List<FilteredText> p_277990_, CallbackInfo ci) {
        ((ServerPlayer) player).connection.send(this.getUpdatePacket());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private SignText setMessages(net.minecraft.world.entity.player.Player entityhuman, List<FilteredText> list, SignText signtext) {
        for (int i = 0; i < list.size(); ++i) {
            FilteredText filteredtext = list.get(i);
            Style chatmodifier = signtext.getMessage(i, entityhuman.isTextFilteringEnabled()).getStyle();

            if (entityhuman.isTextFilteringEnabled()) {
                signtext = signtext.setMessage(i, Component.literal(filteredtext.filteredOrEmpty()).setStyle(chatmodifier));
            } else {
                signtext = signtext.setMessage(i, Component.literal(filteredtext.raw()).setStyle(chatmodifier), Component.literal(filteredtext.filteredOrEmpty()).setStyle(chatmodifier));
            }

            // CraftBukkit start
            org.bukkit.entity.Player player = ((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity();
            String[] lines = new String[4];

            for (int j = 0; j < list.size(); ++j) {
                lines[j] = CraftChatMessage.fromComponent(signtext.getMessage(j, entityhuman.isTextFilteringEnabled()));
            }

            SignChangeEvent event = new SignChangeEvent(CraftBlock.at(this.level, this.worldPosition), player, lines);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                Component[] components = CraftSign.sanitizeLines(event.getLines());
                for (int j = 0; j < components.length; j++) {
                    signtext = signtext.setMessage(j, components[j]);
                }
            }
            // CraftBukkit end
        }

        return signtext;
    }

    @Redirect(method = "executeClickCommandsIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;createCommandSourceStack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/commands/CommandSourceStack;"))
    private CommandSourceStack arclight$setSource(Player p_279428_, Level p_279359_, BlockPos p_279430_) {
        var stack = createCommandSourceStack(p_279428_, p_279359_, p_279430_);
        ((CommandSourceBridge) stack).bridge$setSource(this);
        return stack;
    }

    @Inject(method = "markUpdated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V"))
    public void arclight$setColor(CallbackInfo ci) {
        if (this.level == null) {
            ci.cancel();
        }
    }

    @Override
    public void sendSystemMessage(@NotNull Component component) {
    }

    @Override
    public boolean acceptsSuccess() {
        return false;
    }

    @Override
    public boolean acceptsFailure() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return wrapper.getEntity() != null ? ((EntityBridge) wrapper.getEntity()).bridge$getBukkitSender(wrapper) : new CraftBlockCommandSender(wrapper, (BlockEntity) (Object) this);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }
}
