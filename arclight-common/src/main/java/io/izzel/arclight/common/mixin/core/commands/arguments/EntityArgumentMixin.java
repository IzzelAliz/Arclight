package io.izzel.arclight.common.mixin.core.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.izzel.arclight.common.bridge.core.command.arguments.EntityArgumentBridge;
import io.izzel.arclight.common.bridge.core.command.arguments.EntitySelectorParserBridge;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.commands.arguments.EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED;
import static net.minecraft.commands.arguments.EntityArgument.ERROR_NOT_SINGLE_ENTITY;
import static net.minecraft.commands.arguments.EntityArgument.ERROR_NOT_SINGLE_PLAYER;

@Mixin(EntityArgument.class)
public class EntityArgumentMixin implements EntityArgumentBridge {

    // @formatter:off
    @Shadow @Final boolean single;
    @Shadow @Final boolean playersOnly;
    // @formatter:on

    @Override
    public EntitySelector bridge$parse(StringReader reader, boolean overridePermissions) throws CommandSyntaxException {
        return this.parse(reader, overridePermissions);
    }

    public EntitySelector parse(StringReader reader, boolean overridePermissions) throws CommandSyntaxException {
        int i = 0;
        EntitySelectorParser entityselectorparser = new EntitySelectorParser(reader);
        EntitySelector entityselector = ((EntitySelectorParserBridge) entityselectorparser).bridge$parse(overridePermissions);
        if (entityselector.getMaxResults() > 1 && this.single) {
            if (this.playersOnly) {
                reader.setCursor(0);
                throw ERROR_NOT_SINGLE_PLAYER.createWithContext(reader);
            } else {
                reader.setCursor(0);
                throw ERROR_NOT_SINGLE_ENTITY.createWithContext(reader);
            }
        } else if (entityselector.includesEntities() && this.playersOnly && !entityselector.isSelfSelector()) {
            reader.setCursor(0);
            throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(reader);
        } else {
            return entityselector;
        }
    }
}
