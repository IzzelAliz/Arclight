package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.tileentity.SignTileEntityBridge;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntityMixin implements SignTileEntityBridge, CommandSource, ICommandSourceBridge {

    // @formatter:off
    @Override @Accessor("isEditable") public abstract void bridge$setEditable(boolean editable);
    // @formatter:on

    @Redirect(method = "createCommandSourceStack", at = @At(value = "NEW", target = "net/minecraft/commands/CommandSourceStack"))
    private CommandSourceStack arclight$source(CommandSource source, Vec3 vec3d, Vec2 vec2f, ServerLevel world, int i, String s, Component component, MinecraftServer server, @Nullable Entity entity) {
        return new CommandSourceStack(this, vec3d, vec2f, world, i, s, component, server, entity);
    }

    @Inject(method = "markUpdated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V"))
    public void arclight$setColor(CallbackInfo ci) {
        if (this.level == null) {
            ci.cancel();
        }
    }

    @Override
    public void sendMessage(@NotNull Component component, @NotNull UUID uuid) {
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
