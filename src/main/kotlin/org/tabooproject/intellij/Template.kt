package org.tabooproject.intellij

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tabooproject.intellij.step.ConfigurationPropertiesStep
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
    TemplateFile("settings.gradle.kts"),
).associateBy { it.node }

object Template {

    // GitHub 源
    private const val DOWNLOAD_URL = "https://github.com/TabooLib/taboolib-sdk/archive/refs/heads/idea-template.zip"

    fun downloadAndUnzipFile(baseDir: String, url: String = DOWNLOAD_URL) {
        val response = OkHttpClient()
            .newCall(getRequest(url))
            .execute()
            .takeIf { it.isSuccessful } ?: throw IOException("Failed to download file")

        response.body?.byteStream()?.let { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name.substringAfter("taboolib-sdk-idea-template/")
                        if (entryName.endsWith(".ftl")) {
                            val contentBytes = zip.readBytes()
                            processEntry(entryName, contentBytes, baseDir)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("Response body is null")
    }

    private fun processEntry(entryName: String, contentBytes: ByteArray, baseDir: String) {
        val path = entryName.replace(".ftl", "")
        val templateFile = TEMPLATE_FILES[path] ?: return
        val data = FunctionTemplate.getBuildProperty()

        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            templateLoader = StringTemplateLoader().also { it.putTemplate(templateFile.node, String(contentBytes, StandardCharsets.UTF_8)) }
            defaultEncoding = "UTF-8"
        }

        val finalContent = StringWriter().use { writer ->
            Template(templateFile.node, StringReader(String(contentBytes, StandardCharsets.UTF_8)), cfg).process(data, writer)
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

    private fun getRequest(url: String): Request {
        return Request.Builder().url(url).build()
    }
}