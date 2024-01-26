package io.izzel.arclight.gradle

import groovy.json.JsonOutput
import io.izzel.arclight.gradle.tasks.BuildSpigotTask
import io.izzel.arclight.gradle.tasks.DownloadBuildToolsTask
import io.izzel.arclight.gradle.tasks.ProcessMappingTask
import io.izzel.arclight.gradle.tasks.RemapSpigotTask
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.StopExecutionException

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

class ArclightGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(net.fabricmc.loom.bootstrap.LoomGradlePluginBootstrap)
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
        arclightExt.mappingsConfiguration.bukkitToForge = forgeMappings
        arclightExt.mappingsConfiguration.reobfBukkitPackage = reobfMappings
        arclightExt.mappingsConfiguration.bukkitToForgeInheritance = forgeInheritance

        project.afterEvaluate {
            setupSpigot(project, arclightRepo)
        }
        if (true) return
        def processMapping = project.tasks.create('processMapping', ProcessMappingTask)
        def remapSpigot = project.tasks.create('remapSpigotJar', RemapSpigotTask)
        def generateMeta = project.tasks.create('generateArclightMeta', Copy)
        //def processAt = project.tasks.create('processAT', ProcessAccessTransformerTask)
        def downloadInstaller = project.tasks.create('downloadInstaller')
        generateMeta.dependsOn(downloadInstaller)
        def metaFolder = project.file("${project.buildDir}/arclight_cache/meta")
        project.sourceSets.main.output.dir metaFolder, builtBy: generateMeta
        generateMeta.configure { Copy task ->
            task.into(metaFolder)
            task.outputs.upToDateWhen { false }
            task.dependsOn(remapSpigot)
        }
        project.afterEvaluate {
            if (true) return
            //processAt.configure { ProcessAccessTransformerTask task ->
            //    task.buildData = new File(buildTools, 'BuildData')
            //    task.mcVersion = arclightExt.mcVersion
            //    task.outDir = project.file("${project.buildDir}/arclight_cache/tmp_srg")
            //    task.inSrg = extractSrg.output.get().asFile
            //    task.dependsOn(extractSrg, createSrgToMcp, buildSpigot)
            //}
            def installerJar = project.file("${project.buildDir}/arclight_cache/forge-${arclightExt.mcVersion}-${arclightExt.forgeVersion}-installer.jar")
            downloadInstaller.doFirst {
                if (installerJar.exists()) throw new StopExecutionException()
                if (installerJar.parentFile != null) {
                    installerJar.parentFile.mkdirs()
                }
                def installerUrl = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/${arclightExt.mcVersion}-${arclightExt.forgeVersion}/forge-${arclightExt.mcVersion}-${arclightExt.forgeVersion}-installer.jar"
                Utils.download(installerUrl, installerJar)
            }
            generateMeta.configure { Copy task ->
                task.doFirst {
                    Files.walkFileTree(metaFolder.toPath(), new SimpleFileVisitor<Path>() {
                        @Override
                        FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file)
                            return FileVisitResult.CONTINUE
                        }

                        @Override
                        FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir)
                            return FileVisitResult.CONTINUE
                        }
                    })
                }
                task.into(metaFolder) {
                    task.from(project.zipTree(project.file("${project.buildDir}/arclight_cache/spigot-${arclightExt.mcVersion}-mapped-deobf.jar")))
                    task.from(new File(project.file("${project.buildDir}/arclight_cache/tmp_srg"), 'inheritanceMap.txt'))
                    task.from(new File(project.file("${project.buildDir}/arclight_cache/tmp_srg"), 'bukkit_srg.srg'))
                }
                task.outputs.file(new File(metaFolder as File, 'META-INF/installer.json'))
                task.doLast {
                    def installer = new File(metaFolder, 'META-INF/installer.json')
                    def libs = project.configurations.arclight.dependencies.collect {
                        def classifier = null
                        if (it.artifacts) {
                            it.artifacts.each { DependencyArtifact artifact ->
                                if (artifact.classifier) {
                                    classifier = artifact.classifier
                                }
                            }
                        }
                        if (classifier) {
                            return "${it.group}:${it.name}:${it.version}:$classifier"
                        } else {
                            return "${it.group}:${it.name}:${it.version}"
                        }
                    }
                    def output = [
                            installer: [
                                    minecraft: arclightExt.mcVersion,
                                    forge    : arclightExt.forgeVersion,
                                    hash     : ArclightGradlePlugin.sha1(installerJar)
                            ],
                            libraries: ArclightGradlePlugin.artifacts(project, libs)
                    ]
                    installer.text = JsonOutput.toJson(output)
                }
            }
            if (remapSpigot.outDeobf.exists()) {
                project.configurations.implementation.dependencies.add(project.dependencies.create(project.files(remapSpigot.outDeobf)))
            }
            if (arclightExt.reobfVersion) {
                File map = project.file("${project.buildDir}/arclight_cache/tmp_srg/reobf_version_${arclightExt.bukkitVersion}.srg")
                if (!map.exists()) {
                    map.parentFile.mkdirs()
                    map.createNewFile()
                }
                map.text = "PK: org/bukkit/craftbukkit/v org/bukkit/craftbukkit/${arclightExt.bukkitVersion}"
                project.tasks.withType(RenameJarInPlace).each { task ->
                    project.logger.info "Contributing tsrg mappings ({}) to {} in {}", map, task.name, task.project
                    task.extraMappings.from(map)
                }
            }
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
        arclightExt.mappingsConfiguration.bukkitToForge = forgeMappings
        arclightExt.mappingsConfiguration.reobfBukkitPackage = reobfMappings
        arclightExt.mappingsConfiguration.bukkitToForgeInheritance = forgeInheritance

        def spigotDeps = new File(arclightRepo, "io/izzel/arclight/generated/spigot/${arclightExt.mcVersion}")
        def spigotMapped = new File(spigotDeps, "spigot-${arclightExt.mcVersion}-mapped.jar")
        def spigotDeobf = new File(spigotDeps, "spigot-${arclightExt.mcVersion}-deobf.jar")

        if (forgeMappings.exists() && reobfMappings.exists() && forgeInheritance.exists() && spigotDeobf.exists()) {
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

    private static def sha1(file) {
        MessageDigest md = MessageDigest.getInstance('SHA-1')
        file.eachByte 4096, { bytes, size ->
            md.update(bytes, 0 as byte, size)
        }
        return md.digest().collect { String.format "%02x", it }.join()
    }

    private static Map<String, String> artifacts(Project project, List<String> arts) {
        def ret = new HashMap<String, String>()
        def cfg = project.configurations.create("art_rev_" + System.currentTimeMillis())
        cfg.transitive = false
        arts.each {
            def dep = project.dependencies.create(it)
            cfg.dependencies.add(dep)
        }
        cfg.resolve()
        cfg.resolvedConfiguration.resolvedArtifacts.each { it ->
            def art = [
                    group     : it.moduleVersion.id.group,
                    name      : it.moduleVersion.id.name,
                    version   : it.moduleVersion.id.version,
                    classifier: it.classifier,
                    extension : it.extension,
                    file      : it.file
            ]
            def desc = "${art.group}:${art.name}:${art.version}"
            if (art.classifier != null)
                desc += ":${art.classifier}"
            if (art.extension != 'jar')
                desc += "@${art.extension}"
            ret.put(desc.toString(), sha1(art.file))
        }
        return arts.collectEntries { [(it.toString()): ret.get(it.toString())] }
    }
}
