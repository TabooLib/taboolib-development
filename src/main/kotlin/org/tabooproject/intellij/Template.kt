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
            unzipInMemory(inputStream).forEach { (path, stream) ->
                process(path, stream, baseDir)
            }
        } ?: throw IOException("Response body is null")
    }

    private fun unzipInMemory(inputStream: InputStream): List<Pair<String, InputStream>> {
        val fileList = mutableListOf<Pair<String, InputStream>>()

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val fileBytes = zip.readBytes()
                    val entryName = entry.name.substringAfter("taboolib-sdk-idea-template/")
                    fileList.add(entryName to ByteArrayInputStream(fileBytes))
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return fileList
    }

    private fun process(path: String, stream: InputStream, baseDir: String) {
        val rawContent = stream.readBytes().toString(StandardCharsets.UTF_8)
        val replacedPath = path.replace(".ftl", "")
        val templateFile = TEMPLATE_FILES[replacedPath] ?: return
        val content = read(templateFile, rawContent)
        val writtenPath = replacedPath
            .replace("io.github/username/project/ExamplePlugin",
                ConfigurationPropertiesStep.property.mainClass.replace(".", "/")
            )
        createFileWithDirectories(baseDir, writtenPath)?.also { file ->
            Files.write(file, content.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun read(templateFile: TemplateFile, content: String): String {
        val data = FunctionTemplate.getBuildProperty()
        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            templateLoader = StringTemplateLoader()
            defaultEncoding = "UTF-8"
        }
        val template = Template(templateFile.node, StringReader(content), cfg)
        StringWriter().use { writer ->
            template.process(data, writer)
            return writer.toString()
        }
    }

    private fun getRequest(url: String): Request {
        return Request.Builder().url(url).build()
    }
}