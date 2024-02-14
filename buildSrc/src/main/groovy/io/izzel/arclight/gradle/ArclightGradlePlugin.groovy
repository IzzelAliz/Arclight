package io.izzel.arclight.gradle

import io.izzel.arclight.gradle.tasks.BuildSpigotTask
import io.izzel.arclight.gradle.tasks.DownloadBuildToolsTask
import io.izzel.arclight.gradle.tasks.ProcessMappingTask
import io.izzel.arclight.gradle.tasks.RemapSpigotTask
import net.fabricmc.loom.bootstrap.LoomGradlePluginBootstrap
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper
import org.gradle.api.Plugin
import org.gradle.api.Project

class ArclightGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(LoomGradlePluginBootstrap)
        def arclightExt = project.extensions.create('arclight', ArclightExtension, project)
        def arclightRepo = project.rootProject.file("${Project.DEFAULT_BUILD_DIR_NAME}/arclight_repo")
        project.repositories.maven {
            name = "Arclight Spigot Repo"
            url = arclightRepo
        }

        def mappingsDir = project.rootProject.file("${Project.DEFAULT_BUILD_DIR_NAME}/arclight_cache/tmp_srg")
        def forgeMappings = new File(mappingsDir, "bukkit_srg.srg")
        def forgeInheritance = new File(mappingsDir, 'inheritanceMap.txt')
        def reobfMappings = new File(mappingsDir, 'reobf_bukkit.srg')
        def neoforgeMappings = new File(mappingsDir, 'bukkit_moj.srg')
        def fabricMappings = new File(mappingsDir, 'bukkit_intermediary.srg')
        def fabricInheritance = new File(mappingsDir, 'inheritanceMap_intermediary.txt')
        arclightExt.mappingsConfiguration.bukkitToForge = forgeMappings
        arclightExt.mappingsConfiguration.reobfBukkitPackage = reobfMappings
        arclightExt.mappingsConfiguration.bukkitToForgeInheritance = forgeInheritance
        arclightExt.mappingsConfiguration.bukkitToNeoForge = neoforgeMappings
        arclightExt.mappingsConfiguration.bukkitToFabric = fabricMappings
        arclightExt.mappingsConfiguration.bukkitToFabricInheritance = fabricInheritance

        project.afterEvaluate {
            setupSpigot(project, arclightRepo)
        }
    }

    private static def setupSpigot(Project project, File arclightRepo) {
        def arclightExt = project.extensions.getByName('arclight') as ArclightExtension
        def buildTools = project.rootProject.file("${Project.DEFAULT_BUILD_DIR_NAME}/arclight_cache/buildtools")
        def buildToolsFile = new File(buildTools, 'BuildTools.jar')

        def mappingsDir = project.rootProject.file("${Project.DEFAULT_BUILD_DIR_NAME}/arclight_cache/tmp_srg")
        def forgeMappings = new File(mappingsDir, "bukkit_srg.srg")
        def forgeInheritance = new File(mappingsDir, 'inheritanceMap.txt')
        def reobfMappings = new File(mappingsDir, 'reobf_bukkit.srg')
        def neoforgeMappings = new File(mappingsDir, 'bukkit_moj.srg')
        def fabricMappings = new File(mappingsDir, 'bukkit_intermediary.srg')
        def fabricInheritance = new File(mappingsDir, 'inheritanceMap_intermediary.txt')

        def spigotDeps = new File(arclightRepo, "io/izzel/arclight/generated/spigot/${arclightExt.mcVersion}")
        def spigotMapped = new File(spigotDeps, "spigot-${arclightExt.mcVersion}-mapped.jar")
        def spigotDeobf = new File(spigotDeps, "spigot-${arclightExt.mcVersion}-deobf.jar")

        if (forgeMappings.exists() && reobfMappings.exists() && forgeInheritance.exists() && neoforgeMappings.exists()
                && fabricInheritance.exists() && fabricMappings.exists() && spigotDeobf.exists()) {
            return
        }
        project.logger.lifecycle(":step1 download build tools")
        def downloadSpigot = new DownloadBuildToolsTask()
        downloadSpigot.output = buildToolsFile
        downloadSpigot.run()

        project.logger.lifecycle(":step2 build spigot")
        def buildSpigot = new BuildSpigotTask(project)
        buildSpigot.outputDir = spigotDeps
        buildSpigot.workDir = buildTools
        buildSpigot.mcVersion = arclightExt.mcVersion
        buildSpigot.buildTools = buildToolsFile
        buildSpigot.run()

        new LocalMavenHelper("io.izzel.arclight.generated", "spigot", arclightExt.mcVersion, null, arclightRepo.toPath()).savePom()

        project.logger.lifecycle(":step3 process mappings")
        def processMapping = new ProcessMappingTask(project)
        processMapping.buildData = new File(buildTools, 'BuildData')
        processMapping.mcVersion = arclightExt.mcVersion
        processMapping.bukkitVersion = arclightExt.bukkitVersion
        processMapping.outDir = mappingsDir
        processMapping.inJar = buildSpigot.outSpigot
        processMapping.run()

        project.logger.lifecycle(":step4 remap spigot jar")
        def remapSpigot = new RemapSpigotTask(project)
        remapSpigot.ssJar = new File(buildTools, 'BuildData/bin/SpecialSource.jar')
        remapSpigot.inJar = buildSpigot.outSpigot
        remapSpigot.inSrg = new File(processMapping.outDir, 'bukkit_srg.srg')
        remapSpigot.inSrgToStable = new File(processMapping.outDir, "srg_to_named.srg")
        remapSpigot.inheritanceMap = new File(processMapping.outDir, 'inheritanceMap.txt')
        remapSpigot.outJar = project.file(spigotMapped)
        remapSpigot.outDeobf = project.file(spigotDeobf)
        remapSpigot.inAt = arclightExt.accessTransformer
        remapSpigot.bukkitVersion = arclightExt.bukkitVersion
        remapSpigot.inExtraSrg = arclightExt.extraMapping
        remapSpigot.run()
    }
}
