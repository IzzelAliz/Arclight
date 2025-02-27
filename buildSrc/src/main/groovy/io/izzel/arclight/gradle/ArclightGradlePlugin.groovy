package io.izzel.arclight.gradle

import io.izzel.arclight.gradle.extension.ArclightExtension
import io.izzel.arclight.gradle.runnable.FileDownloader
import io.izzel.arclight.gradle.runnable.SpigotBuilder
import io.izzel.arclight.gradle.tasks.ProcessMappingTask
import io.izzel.arclight.gradle.tasks.RemapSpigotTask
import net.fabricmc.loom.bootstrap.LoomGradlePluginBootstrap
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Path

class ArclightGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(LoomGradlePluginBootstrap)
        def arclight = project.extensions.create('arclight', ArclightExtension, project)

        def arclightRepo = arclight.cacheDir.resolve('arclight_repo')
        project.repositories.maven {
            name = 'Arclight Spigot Repo'
            url = arclightRepo
        }

        def mappingsDir = arclight.cacheDir.resolve('arclight_cache/mappings')
        def forgeMappings = mappingsDir.resolve('bukkit_srg.srg').toFile()
        def forgeInheritance = mappingsDir.resolve('inheritanceMap.txt').toFile()
        def reobfMappings = mappingsDir.resolve('reobf_bukkit.srg').toFile()
        def neoforgeMappings = mappingsDir.resolve('bukkit_moj.srg').toFile()
        def fabricMappings = mappingsDir.resolve('bukkit_intermediary.srg').toFile()
        def fabricInheritance = mappingsDir.resolve('inheritanceMap_intermediary.txt').toFile()
        arclight.mappingsConfiguration.bukkitToForge = forgeMappings
        arclight.mappingsConfiguration.reobfBukkitPackage = reobfMappings
        arclight.mappingsConfiguration.bukkitToForgeInheritance = forgeInheritance
        arclight.mappingsConfiguration.bukkitToNeoForge = neoforgeMappings
        arclight.mappingsConfiguration.bukkitToFabric = fabricMappings
        arclight.mappingsConfiguration.bukkitToFabricInheritance = fabricInheritance

        project.afterEvaluate {
            setupSpigot(project, arclightRepo)
        }
    }

    private static def setupSpigot(Project project, Path arclightRepo) {
        def arclight = project.extensions.getByName('arclight') as ArclightExtension

        def buildTools = arclight.cacheDir.resolve('arclight_cache/buildtools')
        def buildToolsJar = buildTools.resolve('BuildTools.jar')

        def mappingsDir = arclight.cacheDir.resolve('arclight_cache/mappings')

        def spigotDeps = arclightRepo.resolve("io/izzel/arclight/generated/spigot/${arclight.mcVersion}")
        def spigotMapped = spigotDeps.resolve("spigot-${arclight.mcVersion}-mapped.jar")
        def spigotDeobf = spigotDeps.resolve("spigot-${arclight.mcVersion}-deobf.jar")

        if (arclight.mappingsConfiguration.areMappingsExist()
                && Files.exists(spigotDeobf)) {
            // Todo: always run spigot builder.
            return
        }

        project.logger.lifecycle(":step1 download build tools")
        def downloadBuildTools = new FileDownloader("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar", buildToolsJar)
        downloadBuildTools.run()

        project.logger.lifecycle(":step2 build spigot")
        def spigotBuilder = project.getObjects().newInstance(SpigotBuilder)
        spigotBuilder.buildToolsJar = buildToolsJar
        spigotBuilder.workDir = buildTools
        spigotBuilder.outputDir = spigotDeps
        spigotBuilder.minecraftVersion = arclight.mcVersion
        spigotBuilder.run()

        new LocalMavenHelper("io.izzel.arclight.generated", "spigot", arclight.mcVersion, null, arclightRepo).savePom()

        project.logger.lifecycle(":step3 process mappings")
        def processMapping = new ProcessMappingTask(project)
        processMapping.buildData = new File(buildTools.toFile(), 'BuildData')
        processMapping.mcVersion = arclight.mcVersion
        processMapping.bukkitVersion = arclight.bukkitVersion
        processMapping.outDir = mappingsDir.toFile()
        processMapping.inJar = spigotBuilder.outputJar.toFile()
        processMapping.run()

        project.logger.lifecycle(":step4 remap spigot jar")
        def remapSpigot = new RemapSpigotTask(project)
        remapSpigot.ssJar = new File(buildTools.toFile(), 'BuildData/bin/SpecialSource.jar')
        remapSpigot.inJar = spigotBuilder.outputJar.toFile()
        remapSpigot.inSrg = new File(processMapping.outDir, 'bukkit_srg.srg')
        remapSpigot.inSrgToStable = new File(processMapping.outDir, "srg_to_named.srg")
        remapSpigot.inheritanceMap = new File(processMapping.outDir, 'inheritanceMap.txt')
        remapSpigot.outJar = project.file(spigotMapped)
        remapSpigot.outDeobf = project.file(spigotDeobf)
        remapSpigot.inAt = arclight.accessTransformer
        remapSpigot.bukkitVersion = arclight.bukkitVersion
        remapSpigot.inExtraSrg = arclight.extraMapping
        remapSpigot.run()
    }
}
