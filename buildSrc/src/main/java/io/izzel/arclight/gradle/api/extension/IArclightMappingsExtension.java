package io.izzel.arclight.gradle.api.extension;

import java.io.File;

public interface IArclightMappingsExtension {
    File getBukkitToForge();
    void setBukkitToForge(File bukkitToForge);

    File getBukkitToNeoForge();
    void setBukkitToNeoForge(File bukkitToNeoForge);

    File getBukkitToFabric();
    void setBukkitToFabric(File bukkitToFabric);

    File getBukkitToFabricInheritance();
    void setBukkitToFabricInheritance(File bukkitToFabricInheritance);

    File getBukkitToForgeInheritance();
    void setBukkitToForgeInheritance(File bukkitToForgeInheritance);

    File getReobfBukkitPackage();
    void setReobfBukkitPackage(File reobfBukkitPackage);

    default boolean areMappingsExist() {
        return getBukkitToForge().exists()
                && getBukkitToNeoForge().exists()
                && getBukkitToFabric().exists()
                && getBukkitToFabricInheritance().exists()
                && getBukkitToForgeInheritance().exists()
                && getReobfBukkitPackage().exists();
    }
}
