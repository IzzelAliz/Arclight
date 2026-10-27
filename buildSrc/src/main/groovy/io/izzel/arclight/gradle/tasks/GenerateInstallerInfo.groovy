package io.izzel.arclight.gradle.tasks

import groovy.json.JsonOutput
import io.izzel.arclight.gradle.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

class GenerateInstallerInfo extends DefaultTask {

    private String minecraftVersion, forgeVersion, neoforgeVersion, fabricLoaderVersion
    private Configuration configuration, fabricExtra

    @Classpath
    Configuration getConfiguration() {
        return configuration
    }

    void setConfiguration(Configuration configuration) {
        this.configuration = configuration
    }

    @Classpath
    Configuration getFabricExtra() {
        return fabricExtra
    }

    void setFabricExtra(Configuration fabricExtra) {
        this.fabricExtra = fabricExtra
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

    @Input
    String getNeoforgeVersion() {
        return neoforgeVersion
    }

    void setNeoforgeVersion(String neoforgeVersion) {
        this.neoforgeVersion = neoforgeVersion
    }

    @Input
    String getFabricLoaderVersion() {
        return fabricLoaderVersion
    }

    void setFabricLoaderVersion(String fabricLoaderVersion) {
        this.fabricLoaderVersion = fabricLoaderVersion
    }

    private static Map<String, String> configurationArtifacts(Configuration conf) {
        def artifacts = new TreeMap<String, String>()
        conf.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            if (artifact.id.componentIdentifier instanceof ModuleComponentIdentifier && artifact.extension == 'jar') {
                def module = artifact.moduleVersion.id
                def coordinate = "${module.group}:${module.name}:${module.version}"
                if (artifact.classifier) {
                    coordinate += ":${artifact.classifier}"
                }
                artifacts.put(coordinate.toString(), Utils.sha1(artifact.file))
            }
        }
        return artifacts
    }

    @TaskAction
    void run() {
        def forgeUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/$minecraftVersion-$forgeVersion/forge-$minecraftVersion-$forgeVersion-installer.jar"
        def tmpForge = Files.createTempFile("forge", "jar")
        Utils.download(forgeUrl, tmpForge.toFile())
        def neoforgeUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoforgeVersion/neoforge-$neoforgeVersion-installer.jar"
        def tmpNeoforge = Files.createTempFile("neoforge", "jar")
        Utils.download(neoforgeUrl, tmpNeoforge.toFile())
        def fabricLoaderUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/$fabricLoaderVersion/fabric-loader-${fabricLoaderVersion}.jar"
        def tmpFabric = Files.createTempFile("fabric", "jar")
        Utils.download(fabricLoaderUrl, tmpFabric.toFile())
        def libraries = configurationArtifacts(this.configuration)
        def fabricLibraries = configurationArtifacts(this.fabricExtra)
        libraries.each { coordinate, hash ->
            if (fabricLibraries.get(coordinate) != hash) {
                throw new IllegalStateException("Fabric installer overrides shared library ${coordinate}")
            }
            fabricLibraries.remove(coordinate)
        }
        def output = [
                installer  : [
                        minecraft       : minecraftVersion,
                        forge           : forgeVersion,
                        forgeHash       : Utils.sha1(tmpForge.toFile()),
                        neoforge        : neoforgeVersion,
                        neoforgeHash    : Utils.sha1(tmpNeoforge.toFile()),
                        fabricLoader    : fabricLoaderVersion,
                        fabricLoaderHash: Utils.sha1(tmpFabric.toFile()),
                ],
                libraries  : libraries,
                fabricExtra: fabricLibraries
        ]
        outputs.files.singleFile.text = JsonOutput.toJson(output)
    }
}
