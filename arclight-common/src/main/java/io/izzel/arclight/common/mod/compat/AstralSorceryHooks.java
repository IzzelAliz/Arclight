package io.izzel.arclight.common.mod.compat;

import net.minecraft.world.entity.Entity;

public class AstralSorceryHooks {

    private static Class<?> interactClass;

    static {
        try {
            interactClass = Class.forName("hellfirepvp.astralsorcery.common.entity.InteractableEntity");
        } catch (ClassNotFoundException e) {
            interactClass = null;
        }
    }

    public static boolean notInteractable(Entity entity) {
        return interactClass == null || !interactClass.isInstance(entity);
    }
}
