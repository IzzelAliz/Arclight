package io.izzel.arclight.common.bridge.core.server.packs.repository;

import net.minecraft.world.level.validation.DirectoryValidator;

import java.util.Collection;

public interface PackRepositoryBridge {

    void bridge$setDatapackValidator(DirectoryValidator validator);

    void bridge$reload(boolean pendingReload);

    void bridge$setSelected(Collection<String> packs, boolean pendingReload);
}
