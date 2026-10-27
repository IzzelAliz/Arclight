package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.entity.SignBlockEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.bukkit.Bukkit;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntityMixin implements SignBlockEntityBridge, CommandSource, CommandSourceBridge {

    // @formatter:off
    @Shadow public abstract ClientboundBlockEntityDataPacket getUpdatePacket();
    @Shadow public abstract boolean isWaxed();
    @Shadow @javax.annotation.Nullable public abstract UUID getPlayerWhoMayEdit();
    @Shadow public abstract boolean updateText(UnaryOperator<SignText> p_277877_, boolean p_277426_);
    @Shadow public abstract void setAllowedPlayerEditor(@Nullable UUID p_155714_);
    @Shadow @Final private static Logger LOGGER;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void updateSignText(Player p_278048_, boolean p_278103_, List<FilteredText> p_277990_) {
        if (!this.isWaxed() && p_278048_.getUUID().equals(this.getPlayerWhoMayEdit()) && this.level != null) {
            this.updateText((p_277776_) -> {
                return this.setMessages(p_278048_, p_277990_, p_277776_, p_278103_);
            }, p_278103_);
            this.setAllowedPlayerEditor(null);
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        } else {
            LOGGER.warn("Player {} just tried to change non-editable sign", p_278048_.getName().getString());
            ((ServerPlayer) p_278048_).connection.send(this.getUpdatePacket());
        }
    }

    private SignText setMessages(net.minecraft.world.entity.player.Player entityhuman, List<FilteredText> list, SignText signtext, boolean front) {
        SignText orig = signtext;
        for (int i = 0; i < list.size(); ++i) {
            FilteredText filteredtext = list.get(i);
            Style chatmodifier = signtext.getMessage(i, entityhuman.isTextFilteringEnabled()).getStyle();

            if (entityhuman.isTextFilteringEnabled()) {
                signtext = signtext.setMessage(i, Component.literal(filteredtext.filteredOrEmpty()).setStyle(chatmodifier));
            } else {
                signtext = signtext.setMessage(i, Component.literal(filteredtext.raw()).setStyle(chatmodifier), Component.literal(filteredtext.filteredOrEmpty()).setStyle(chatmodifier));
            }
        }

        // CraftBukkit start
        org.bukkit.entity.Player player = ((ServerPlayerBridge) entityhuman).bridge$getBukkitEntity();
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>();

        for (int j = 0; j < list.size(); ++j) {
            lines.add(PaperAdventure.asAdventure(signtext.getMessage(j, entityhuman.isTextFilteringEnabled())));
        }

        SignChangeEvent event = new SignChangeEvent(CraftBlock.at(this.level, this.worldPosition), player, new ArrayList<>(lines), front ? Side.FRONT : Side.BACK);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return orig;
        }

        Component[] components = CraftSign.sanitizeLines(event.lines());
        for (int j = 0; j < components.length; j++) {
            if (!Objects.equals(lines.get(j), event.line(j))) {
                signtext = signtext.setMessage(j, components[j]);
            }
        }
        // CraftBukkit end

        return signtext;
    }

    @Decorate(method = "executeClickCommandsIfPresent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Z)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;createCommandSourceStack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/commands/CommandSourceStack;"))
    private CommandSourceStack arclight$signCommand(Player player, ServerLevel level, BlockPos pos,
                                                   @Local(ordinal = 0) boolean front,
                                                   @Local(ordinal = 0) ClickEvent.RunCommand command) throws Throwable {
        String commandLine = command.command().startsWith("/") ? command.command() : "/" + command.command();
        if (org.spigotmc.SpigotConfig.logCommands) {
            LOGGER.info("{} issued server command: {}", player.getScoreboardName(), commandLine);
        }
        var event = new PlayerSignCommandPreprocessEvent(
            (org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity(), commandLine,
            new LazyPlayerSet(level.getServer()),
            (org.bukkit.block.Sign) CraftBlock.at(this.level, this.worldPosition).getState(),
            front ? Side.FRONT : Side.BACK);
        if (!event.callEvent()) {
            // The enclosing boolean host must stop the remaining sign lines as well.
            return (CommandSourceStack) DecorationOps.cancel().invoke(false);
        }
        // The native command.command() read is still ahead of this source-creation callsite.
        // Rewrite only this line's local, keeping the native dispatcher and other click actions.
        command = new ClickEvent.RunCommand(event.getMessage());
        DecorationOps.blackhole().invoke(command);
        Player commandPlayer = ((CraftPlayer) event.getPlayer()).getHandle();
        var stack = (CommandSourceStack) DecorationOps.callsite().invoke(commandPlayer, level, pos);
        ((CommandSourceStackBridge) stack).bridge$setSource(this);
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
