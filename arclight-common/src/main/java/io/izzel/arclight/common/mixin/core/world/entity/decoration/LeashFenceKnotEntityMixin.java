package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.mixin.core.world.entity.decoration.BlockAttachedEntityMixin;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin extends BlockAttachedEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public InteractionResult interact(final Player entityhuman, final InteractionHand enumhand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (entityhuman.getItemInHand(enumhand).is(Items.SHEARS)) {
            InteractionResult interactionresult = InteractionResult.PASS;

            if (interactionresult instanceof InteractionResult.Success interactionresult_success && interactionresult_success.wasItemInteraction()) {
                return interactionresult;
            }
        }

        boolean flag = false;
        for (var leashable : Leashable.leashableLeashedTo(entityhuman)) {
            if (leashable.canHaveALeashAttachedTo((LeashFenceKnotEntity) (Object) this)) {
                if (leashable instanceof Entity entity) {
                    if (CraftEventFactory.callPlayerLeashEntityEvent(entity, (LeashFenceKnotEntity) (Object) this, entityhuman, enumhand).isCancelled()) {
                        ((ServerPlayer) entityhuman).connection.send(new ClientboundSetEntityLinkPacket(entity, leashable.getLeashHolder()));
                        flag = true;
                        continue;
                    }
                }
                leashable.setLeashedTo((LeashFenceKnotEntity) (Object) this, true);
                flag = true;
            }
        }
        boolean flag1 = false;
        if (!flag && !entityhuman.isSecondaryUseActive()) {
            for (var leashable : Leashable.leashableLeashedTo((LeashFenceKnotEntity) (Object) this)) {
                if (leashable instanceof Entity entity) {
                    if (CraftEventFactory.callPlayerUnleashEntityEvent(entity, entityhuman, enumhand).isCancelled()) {
                        continue;
                    }
                }
                leashable.setLeashedTo(entityhuman, true);
                flag1 = true;
            }
        }
        if (flag || flag1) {
            this.gameEvent(GameEvent.BLOCK_ATTACH, entityhuman);
            this.playSound(SoundEvents.LEAD_TIED, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
