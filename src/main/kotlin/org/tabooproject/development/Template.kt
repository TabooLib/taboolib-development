package org.tabooproject.development

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import org.tabooproject.development.step.ConfigurationPropertiesStep
import org.tabooproject.development.step.TEMPLATE_DOWNLOAD_MIRROR
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

data class TemplateFile(val node: String)

val TEMPLATE_FILES: Map<String, TemplateFile> = listOf(
    TemplateFile(".github/.workflows/main.yml"),
    TemplateFile("gradle/wrapper/gradle-wrapper.properties"),
    TemplateFile("src/main/kotlin/io.github/username/project/ExamplePlugin.kt"),
    TemplateFile(".gitignore"),
    TemplateFile("LICENSE"),
    TemplateFile("README.md"),
    TemplateFile("build.gradle.kts"),
    TemplateFile("gradle.properties"),
    TemplateFile("settings.gradle.kts")
).associateBy { it.node }

object Template {

    private fun getTemplateDownloadUrl(): String {
        return TEMPLATE_DOWNLOAD_MIRROR.getValue(ConfigurationPropertiesStep.property.mirrorIndex)
    }

    fun downloadAndUnzipFile(baseDir: String, url: String = getTemplateDownloadUrl()) {
        val client = createOkHttpClientWithSystemProxy {
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(10, TimeUnit.SECONDS)
        }

        val response = client
            .newCall(getRequest(url))
            .execute()
            .takeIf { it.isSuccessful } ?: throw IOException("Failed to download file")

        // only get once version
        val data = FunctionTemplate.getBuildProperty()

        response.body?.byteStream()?.let { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name.substringAfter("taboolib-sdk-idea-template/")
                        if (entryName.endsWith(".ftl")) {
                            val contentBytes = zip.readBytes()
                            processEntry(entryName, contentBytes, baseDir, data)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("Response body is null")
    }

    private fun processEntry(entryName: String, contentBytes: ByteArray, baseDir: String, buildProperty: Map<String, Any>) {
        val path = entryName.replace(".ftl", "")
        val templateFile = TEMPLATE_FILES[path] ?: return

        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            templateLoader = StringTemplateLoader().also { it.putTemplate(templateFile.node, String(contentBytes, StandardCharsets.UTF_8)) }
            defaultEncoding = "UTF-8"
        }

        val finalContent = StringWriter().use { writer ->
            Template(templateFile.node, StringReader(String(contentBytes, StandardCharsets.UTF_8)), cfg).process(buildProperty, writer)
            writer.toString()
        }

        val replacedPath = path.replace(
            "io.github/username/project/ExamplePlugin",
            ConfigurationPropertiesStep.property.mainClass.replace(".", "/")
        )
        createFileWithDirectories(baseDir, replacedPath)?.also { file ->
            Files.write(file, finalContent.toByteArray(StandardCharsets.UTF_8))
        }
    }
}