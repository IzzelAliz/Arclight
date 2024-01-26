package io.izzel.arclight.gradle.tasks

import io.izzel.arclight.gradle.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class DownloadBuildToolsTask implements Runnable {

    static def URL = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"

    private File output

    File getOutput() {
        return output
    }

    void setOutput(File output) {
        this.output = output
    }

    @Override
    void run() {
        if (output.exists()) {
            return
        }
        Utils.download(URL, output)
    }
}
