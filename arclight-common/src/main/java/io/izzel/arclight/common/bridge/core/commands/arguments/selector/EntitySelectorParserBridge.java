package io.izzel.arclight.common.bridge.core.commands.arguments.selector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.selector.EntitySelector;

public interface EntitySelectorParserBridge {

    EntitySelector bridge$parse(boolean overridePermissions) throws CommandSyntaxException;

    void bridge$parseSelector(boolean overridePermissions) throws CommandSyntaxException;
}
