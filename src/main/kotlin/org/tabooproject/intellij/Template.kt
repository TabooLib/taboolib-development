package org.tabooproject.intellij

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tabooproject.intellij.step.BuildSystemPropertiesStep
import org.tabooproject.intellij.step.ConfigurationPropertiesStep
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

data class TemplateFile(val node: String, val block: () -> Map<String, String> = { mapOf() }) {

    fun getDownloadLink(): String {
        return "https://raw.githubusercontent.com/TabooLib/taboolib-sdk/idea-template/${node}.ftl"
    }
}

object Template {

    private const val TEMPLATE_PREFIX = "TABOO_INTELLIJ_PREFIX"
    private const val TEMPLATE_SUFFIX = "TABOO_INTELLIJ_SUFFIX"

    val WORKFLOW_YML = TemplateFile(".github/.workflows/main.yml")

    val GRADLE_PROPERTY = TemplateFile("gradle.properties") {
        mapOf(
            "group" to BuildSystemPropertiesStep.property.groupId,
            "version" to BuildSystemPropertiesStep.property.version,
            "artifactId" to BuildSystemPropertiesStep.property.artifactId
        )
    }

    val GRADLE_WRAPPER_PROPERTIES = TemplateFile("gradle/wrapper/gradle-wrapper.properties")

    val MAIN_FILE = TemplateFile("src/main/kotlin/io.github/username/project/ExamplePlugin.kt") {
        mapOf(
            "mainClass" to ConfigurationPropertiesStep.property.mainClass,
            "name" to ConfigurationPropertiesStep.property.name
        )
    }

    val GITIGNORE = TemplateFile(".gitignore")

    val LICENSE = TemplateFile("LICENSE")

    val README = TemplateFile("README.md") {
        mapOf("name" to ConfigurationPropertiesStep.property.name)
    }

    val SETTINGS_GRADLE = TemplateFile("settings.gradle.kts") {
        mapOf("artifactId" to BuildSystemPropertiesStep.property.artifactId)
    }


    fun extract(file: TemplateFile): String {
        val content = download(file)
        return read(file, content)
    }

    private fun download(file: TemplateFile): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(file.getDownloadLink()).build()
        client.newCall(request).execute().use { response ->
            response.body ?: throw IOException("Download failed")
            return response.body!!.string()
        }
    }

    private fun read(templateFile: TemplateFile, content: String): String {
        val file = Files.createTempFile(TEMPLATE_PREFIX, TEMPLATE_SUFFIX).toFile().apply {
            deleteOnExit()
        }
        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            templateLoader = StringTemplateLoader()
            defaultEncoding = "UTF-8"
        }
        val template = Template(templateFile.node, StringReader(content), cfg)
        StringWriter().use { writer ->
            template.process(templateFile.block(), writer)
            Files.write(Paths.get(file.toURI()), writer.toString().toByteArray(StandardCharsets.UTF_8))
        }
        return file.absolutePath
    }
}