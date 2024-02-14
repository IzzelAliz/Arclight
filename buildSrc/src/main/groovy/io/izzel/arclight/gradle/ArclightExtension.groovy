package io.izzel.arclight.gradle

import org.gradle.api.Project

class ArclightExtension {

    private final Project project
    private String mcVersion
    private String bukkitVersion
    private String forgeVersion
    private File accessTransformer
    private File extraMapping
    private final MappingsConfiguration mappingsConfiguration = new MappingsConfiguration()

    ArclightExtension(Project project) {
        this.project = project
    }

    String getMcVersion() {
        return mcVersion
    }

    void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion
    }

    String getBukkitVersion() {
        return bukkitVersion
    }

    void setBukkitVersion(String bukkitVersion) {
        this.bukkitVersion = bukkitVersion
    }

    File getAccessTransformer() {
        return accessTransformer
    }

    void setAccessTransformer(File accessTransformer) {
        this.accessTransformer = accessTransformer
    }

    String getForgeVersion() {
        return forgeVersion
    }

    void setForgeVersion(String forgeVersion) {
        this.forgeVersion = forgeVersion
    }

    File getExtraMapping() {
        return extraMapping
    }

    void setExtraMapping(File extraMapping) {
        this.extraMapping = extraMapping
    }

    MappingsConfiguration getMappingsConfiguration() {
        return mappingsConfiguration
    }

    static class MappingsConfiguration {

        private File bukkitToForge

        File getBukkitToForge() {
            return bukkitToForge
        }

        void setBukkitToForge(File bukkitToForge) {
            this.bukkitToForge = bukkitToForge
        }

        private File bukkitToNeoForge

        File getBukkitToNeoForge() {
            return bukkitToNeoForge
        }

        void setBukkitToNeoForge(File bukkitToNeoForge) {
            this.bukkitToNeoForge = bukkitToNeoForge
        }

        private File bukkitToFabric

        File getBukkitToFabric() {
            return bukkitToFabric
        }

        void setBukkitToFabric(File bukkitToFabric) {
            this.bukkitToFabric = bukkitToFabric
        }

        private File bukkitToFabricInheritance

        File getBukkitToFabricInheritance() {
            return bukkitToFabricInheritance
        }

        void setBukkitToFabricInheritance(File bukkitToFabricInheritance) {
            this.bukkitToFabricInheritance = bukkitToFabricInheritance
        }
        private File bukkitToForgeInheritance

        File getBukkitToForgeInheritance() {
            return bukkitToForgeInheritance
        }

        void setBukkitToForgeInheritance(File bukkitToForgeInheritance) {
            this.bukkitToForgeInheritance = bukkitToForgeInheritance
        }

        private File reobfBukkitPackage

        File getReobfBukkitPackage() {
            return reobfBukkitPackage
        }

        void setReobfBukkitPackage(File reobfBukkitPackage) {
            this.reobfBukkitPackage = reobfBukkitPackage
        }
    }
}
