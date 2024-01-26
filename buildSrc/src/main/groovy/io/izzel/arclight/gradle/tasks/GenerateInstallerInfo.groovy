package io.izzel.arclight.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.security.MessageDigest

class GenerateInstallerInfo extends DefaultTask {

    private String minecraftVersion, forgeVersion
    private Configuration configuration

    @Classpath
    Configuration getConfiguration() {
        return configuration
    }

    void setConfiguration(Configuration configuration) {
        this.configuration = configuration
    }

    @Input
    String getMinecraftVersion() {
        return minecraftVersion
    }

    void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion
    }

    @Input
    String getForgeVersion() {
        return forgeVersion
    }

    void setForgeVersion(String forgeVersion) {
        this.forgeVersion = forgeVersion
    }

    @TaskAction
    void run() {
        def libs = configuration.dependencies.collect { dep ->
            def classifier = null
            if (dep.artifacts) {
                dep.artifacts.each { DependencyArtifact artifact ->
                    if (artifact.classifier) {
                        classifier = artifact.classifier
                    }
                }
            }
            if (classifier) {
                return "${dep.group}:${dep.name}:${dep.version}:$classifier"
            } else {
                return "${dep.group}:${dep.name}:${dep.version}"
            }
        }
        def sha1 = { file ->
            MessageDigest md = MessageDigest.getInstance('SHA-1')
            file.eachByte 4096, { bytes, size ->
                md.update(bytes, 0 as byte, size)
            }
            return md.digest().collect { String.format "%02x", it }.join()
        }
        def artifacts = { List<String> arts ->
            def ret = new HashMap<String, String>()
            def cfg = project.configurations.create("art_rev_" + System.currentTimeMillis())
            cfg.transitive = false
            arts.each {
                def dep = project.dependencies.create(it)
                cfg.dependencies.add(dep)
            }
            cfg.resolve()
            cfg.resolvedConfiguration.resolvedArtifacts.each { rev ->
                def art = [
                        group     : rev.moduleVersion.id.group,
                        name      : rev.moduleVersion.id.name,
                        version   : rev.moduleVersion.id.version,
                        classifier: rev.classifier,
                        extension : rev.extension,
                        file      : rev.file
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
        def installerUrl = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/$minecraftVersion-$forgeVersion/forge-$minecraftVersion-$forgeVersion-installer.jar"
        def tmpInstaller = Files.createTempFile("installer", "jar")
        installerUrl.toURI().toURL().withReader { r ->
            tmpInstaller.withWriter { w ->
                r.transferTo(w)
            }
        }
        def output = [
                installer: [
                        minecraft: minecraftVersion,
                        forge    : forgeVersion,
                        hash     : sha1(tmpInstaller.toFile())
                ],
                libraries: artifacts(libs)
        ]
        outputs.files.singleFile.text = groovy.json.JsonOutput.toJson(output)
    }
}
