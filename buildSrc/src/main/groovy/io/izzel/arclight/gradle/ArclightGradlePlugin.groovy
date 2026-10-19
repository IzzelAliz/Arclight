package io.izzel.arclight.gradle

import io.izzel.arclight.gradle.extension.ArclightExtension
import io.izzel.arclight.gradle.runnable.FileDownloader
import io.izzel.arclight.gradle.runnable.SpigotBuilder
import io.izzel.arclight.gradle.tasks.ProcessMappingTask
import io.izzel.arclight.gradle.tasks.RemapSpigotTask
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.commons.io.IOUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class ArclightGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def arclight = project.extensions.create('arclight', ArclightExtension, project)

        def arclightRepo = arclight.cacheDir.resolve('arclight_repo')
        project.repositories.maven {
            name = 'Arclight Spigot Repo'
            url = arclightRepo
        }

        project.afterEvaluate {
            setupSpigot(project, arclightRepo)
        }
    }

    private static def setupSpigot(Project project, Path arclightRepo) {
        def arclight = project.extensions.getByName('arclight') as ArclightExtension


        def mappingsDir = arclight.cacheDir.resolve('arclight_cache/mappings')

        def spigotDeps = arclightRepo.resolve("io/izzel/arclight/generated/spigot/${arclight.mcVersion}")
        def spigotMapped = spigotDeps.resolve("spigot-${arclight.mcVersion}-mapped.jar")
        def spigotDeobf = spigotDeps.resolve("spigot-${arclight.mcVersion}-deobf.jar")

        // Generated mappings and bytecode also depend on our build logic and inputs,
        // not just the upstream Spigot revision. Never accept a legacy cache blindly.
        def digest = java.security.MessageDigest.getInstance('SHA-256')
        def inputs = project.rootProject.fileTree('buildSrc/src/main').files +
                project.rootProject.files('build.gradle', 'settings.gradle', 'buildSrc/build.gradle',
                        'gradle/libs.versions.toml', 'gradle/wrapper/gradle-wrapper.properties').files +
                [arclight.accessTransformer, arclight.extraMapping].findAll { it != null }
        inputs.sort { a, b -> a.path <=> b.path }.each { file ->
            digest.update(project.rootProject.relativePath(file).getBytes(StandardCharsets.UTF_8))
            digest.update(file.bytes)
        }
        digest.update(arclight.bukkitVersion.getBytes(StandardCharsets.UTF_8))
        def inputHash = HexFormat.of().formatHex(digest.digest())
        def inputMeta = arclight.cacheDir.resolve('spigot_inputs.sha256')
        def buildMeta = arclight.cacheDir.resolve('spigot_version.json')
        def rev = arclight.mcVersion
        if (arclight.spigotReversion) {
            rev = arclight.spigotReversion
        }

        project.logger.lifecycle("Setup for Spigot ${arclight.mcVersion}(${arclight.spigotReversion})")
        def newBuildMeta = IOUtils.toString(new URI("https://hub.spigotmc.org/versions/${rev}.json").toURL(), StandardCharsets.UTF_8)
        if (Files.exists(buildMeta) && Files.exists(inputMeta)
                && Files.readString(buildMeta) == newBuildMeta
                && Files.readString(inputMeta) == inputHash
                && Files.exists(mappingsDir.resolve('bukkit_at.at'))
                && Files.exists(mappingsDir.resolve('bukkit_aw.aw'))
                && Files.exists(spigotDeobf) && Files.exists(spigotMapped)) {
            project.logger.lifecycle(':spigot build cache valid, using it')
            return
        }

        def buildSpigotWorkDir = arclight.cacheDir.resolve('arclight_cache/buildtools')

        FileUtils.deleteDirectory(buildSpigotWorkDir.toFile())
        Files.createDirectories(buildSpigotWorkDir)

        project.logger.lifecycle(":step1 download build tools")
        def buildToolsJar = buildSpigotWorkDir.resolve('BuildTools.jar')
        def downloadBuildTools = new FileDownloader("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar", buildToolsJar)
        downloadBuildTools.run()

        project.logger.lifecycle(":step2 build spigot")
        def spigotBuilder = project.getObjects().newInstance(SpigotBuilder)
        spigotBuilder.buildToolsJar = buildToolsJar
        spigotBuilder.workDir = buildSpigotWorkDir
        spigotBuilder.outputDir = spigotDeps
        spigotBuilder.minecraftVersion = arclight.mcVersion
        spigotBuilder.reversion = arclight.spigotReversion
        spigotBuilder.run()

        new LocalMavenHelper("io.izzel.arclight.generated", "spigot", arclight.mcVersion, null, arclightRepo).savePom()

        project.logger.lifecycle(":step3 process mappings")
        def processMapping = new ProcessMappingTask(project)
        processMapping.buildData = new File(buildSpigotWorkDir.toFile(), 'BuildData')
        processMapping.mcVersion = arclight.mcVersion
        processMapping.bukkitVersion = arclight.bukkitVersion
        processMapping.outDir = mappingsDir.toFile()
        processMapping.inJar = spigotBuilder.outputJar.toFile()
        processMapping.run()

        project.logger.lifecycle(":step4 remap spigot jar")
        def remapSpigot = new RemapSpigotTask(project)
        remapSpigot.inJar = spigotBuilder.outputJar.toFile()
        remapSpigot.outJar = project.file(spigotMapped)
        remapSpigot.outDeobf = project.file(spigotDeobf)
        remapSpigot.inAt = arclight.accessTransformer
        remapSpigot.bukkitVersion = arclight.bukkitVersion
        remapSpigot.inExtraSrg = arclight.extraMapping
        remapSpigot.run()

        Files.writeString(buildMeta, newBuildMeta)
        Files.writeString(inputMeta, inputHash)
    }
}
