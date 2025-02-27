package io.izzel.arclight.gradle.extension;

import io.izzel.arclight.gradle.api.extension.IArclightMappingsExtension;

import java.io.File;

public class ArclightMappingsExtension implements IArclightMappingsExtension {
    private File bukkitToForge;
    private File bukkitToNeoForge;
    private File bukkitToFabric;
    private File bukkitToFabricInheritance;
    private File bukkitToForgeInheritance;
    private File reobfBukkitPackage;

    @Override
    public File getBukkitToForge() {
        return bukkitToForge;
    }

    @Override
    public void setBukkitToForge(File bukkitToForge) {
        this.bukkitToForge = bukkitToForge;
    }

    @Override
    public File getBukkitToNeoForge() {
        return bukkitToNeoForge;
    }

    @Override
    public void setBukkitToNeoForge(File bukkitToNeoForge) {
        this.bukkitToNeoForge = bukkitToNeoForge;
    }

    @Override
    public File getBukkitToFabric() {
        return bukkitToFabric;
    }

    @Override
    public void setBukkitToFabric(File bukkitToFabric) {
        this.bukkitToFabric = bukkitToFabric;
    }

    @Override
    public File getBukkitToFabricInheritance() {
        return bukkitToFabricInheritance;
    }

    @Override
    public void setBukkitToFabricInheritance(File bukkitToFabricInheritance) {
        this.bukkitToFabricInheritance = bukkitToFabricInheritance;
    }

    @Override
    public File getBukkitToForgeInheritance() {
        return bukkitToForgeInheritance;
    }

    @Override
    public void setBukkitToForgeInheritance(File bukkitToForgeInheritance) {
        this.bukkitToForgeInheritance = bukkitToForgeInheritance;
    }

    @Override
    public File getReobfBukkitPackage() {
        return reobfBukkitPackage;
    }

    @Override
    public void setReobfBukkitPackage(File reobfBukkitPackage) {
        this.reobfBukkitPackage = reobfBukkitPackage;
    }
}
