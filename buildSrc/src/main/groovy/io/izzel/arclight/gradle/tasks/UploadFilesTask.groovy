package io.izzel.arclight.gradle.tasks

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.function.Consumer

abstract class UploadFilesTask extends DefaultTask {

    @Input
    abstract Property<String> getMcVersion()

    @Input
    abstract Property<String> getVersion()

    @Input
    abstract Property<Boolean> getSnapshot()

    @Input
    abstract Property<String> getGitHash()

    @Input
    abstract Property<String> getBranch()

    @TaskAction
    void run() {
        for (def file in inputs.files.asFileTree.files) {
            if (file.isFile()) {
                try {
                    this.uploadOne(file)
                } catch (Exception e) {
                    project.logger.error("Error uploading $file", e)
                    throw e
                }
            }
        }
    }

    private static final String OBJECTS = "https://files.hypoglycemia.icu/v1/objects/%s"
    private static final String FILES = "https://files.hypoglycemia.icu/v1/files%s"

    private void uploadOne(File file) {
        def sha1 = sha1(file)
        def modloader = file.name.split('-')[1]
        project.logger.lifecycle("Uploading {}, sha1 {}", file.name, sha1)
        (new URL(OBJECTS.formatted(sha1)).openConnection() as HttpURLConnection).with {
            it.setRequestMethod("PUT")
            it.doOutput = true
            it.addRequestProperty("X-Lightning-Sha1", sha1)
            it.addRequestProperty("X-Lightning-Filename", file.name.replace(".jar", "-" + gitHash.get() + ".jar"))
            it.addRequestProperty("AuthToken", System.getenv().ARCLIGHT_FILES_TOKEN)
            it.addRequestProperty("Content-Type", "application/java-archive")
            it.addRequestProperty("Content-Length", Files.size(file.toPath()).toString())
            it.setInstanceFollowRedirects(true)
            it.connect()
            using(it.outputStream) {
                Files.copy(file.toPath(), it)
            }
            if (it.responseCode != 200) {
                def reason = new String(it.inputStream.readAllBytes())
                project.logger.error(reason)
                throw new Exception(reason)
            }
        }
        link("/arclight/branches/${branch.get()}/versions-snapshot/${version.get()}/${modloader}", [type: 'object', value: sha1])
        link("/arclight/branches/${branch.get()}/latest-snapshot", [type: 'link', value: "/arclight/branches/${branch.get()}/versions-snapshot/${version.get()}", cache_seconds: 3600])
        if (!snapshot.get()) {
            link("/arclight/branches/${branch.get()}/versions-stable/${version.get()}/${modloader}", [type: 'object', value: sha1])
            link("/arclight/branches/${branch.get()}/latest-stable", [type: 'link', value: "/arclight/branches/${branch.get()}/versions-stable/${version.get()}", cache_seconds: 86400])
        }
    }

    private static void link(String path, Object payload) {
        (new URL(FILES.formatted(path)).openConnection() as HttpURLConnection).with {
            it.setRequestMethod("PUT")
            it.doOutput = true
            it.addRequestProperty("Content-Type", "application/json")
            it.addRequestProperty("AuthToken", System.getenv().ARCLIGHT_FILES_TOKEN)
            it.connect()
            using(it.outputStream) {
                it.write(JsonOutput.toJson(payload).getBytes(StandardCharsets.UTF_8))
            }
            if (it.responseCode != 200) {
                def reason = new String(it.inputStream.readAllBytes())
                project.logger.error(reason)
                throw new Exception(reason)
            }
        }
    }

    static <T extends AutoCloseable> void using(T closeable, Consumer<T> consumer) {
        try {
            consumer.accept(closeable)
        } finally {
            closeable.close()
        }
    }

    static String sha1(File file) {
        MessageDigest md = MessageDigest.getInstance('SHA-1')
        file.eachByte 4096, { bytes, size ->
            md.update(bytes, 0 as byte, size)
        }
        return md.digest().collect { String.format "%02x", it }.join()
    }
}
