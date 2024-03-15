package org.tabooproject.intellij

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tabooproject.intellij.step.BuildSystemPropertiesStep
import org.tabooproject.intellij.step.ConfigurationPropertiesStep
import org.tabooproject.intellij.step.OptionalPropertiesStep
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

data class TemplateFile(val node: String, val block: () -> Map<String, Any> = { mapOf() }) {

    fun getDownloadLink(): String {
//        return "https://raw.githubusercontent.com/TabooLib/taboolib-sdk/idea-template/${node}.ftl"
        return "https://template.tabooproject.org/${node}.ftl"
    }
}

object Template {

    private const val TEMPLATE_PREFIX = "TABOO_INTELLIJ_PREFIX"
    private const val TEMPLATE_SUFFIX = "TABOO_INTELLIJ_SUFFIX"

    val WORKFLOW_YML = TemplateFile(".github/.workflows/main.yml")

    val GRADLE_WRAPPER_PROPERTIES = TemplateFile("gradle/wrapper/gradle-wrapper.properties")

    val MAIN_PLUGIN_KT = TemplateFile("src/main/kotlin/io/github/username/project/ExamplePlugin.kt") {
        mapOf(
            "group" to ConfigurationPropertiesStep.property.mainClass.substringBeforeLast("."),
            "name" to ConfigurationPropertiesStep.property.name
        )
    }

    val GITIGNORE = TemplateFile(".gitignore")

    val LICENSE = TemplateFile("LICENSE")

    val README = TemplateFile("README.md") {
        mapOf("name" to ConfigurationPropertiesStep.property.name)
    }

    val BUILD_GRADLE_KTS = TemplateFile("build.gradle.kts") {
        mutableMapOf<String, Any>(
            "pluginName" to ConfigurationPropertiesStep.property.name,
            "description" to OptionalPropertiesStep.property.description
        ).also {
            if (OptionalPropertiesStep.property.authors.isNotEmpty()) {
                it["authors"] = listOf(OptionalPropertiesStep.property.authors)
            }
            if (OptionalPropertiesStep.property.website.isNotEmpty()) {
                it["website"] = OptionalPropertiesStep.property.website
            }
            if (OptionalPropertiesStep.property.depends.isNotEmpty()) {
                it["dependencies"] = OptionalPropertiesStep.property.depends
            }
            if (OptionalPropertiesStep.property.softDepends.isNotEmpty()) {
                it["softDependencies"] = OptionalPropertiesStep.property.softDepends
            }
        }
    }

    val GRADLE_PROPERTY = TemplateFile("gradle.properties") {
        mapOf(
            "group" to BuildSystemPropertiesStep.property.groupId,
            "version" to BuildSystemPropertiesStep.property.version,
            "artifactId" to BuildSystemPropertiesStep.property.artifactId
        )
    }

    val SETTINGS_GRADLE = TemplateFile("settings.gradle.kts") {
        mapOf("name" to BuildSystemPropertiesStep.property.artifactId)
    }

    fun extract(file: TemplateFile, targetFile: File) {
        val rawContent = download(file)
        val content = read(file, rawContent)
        Files.write(Paths.get(targetFile.toURI()), content.toByteArray(StandardCharsets.UTF_8))
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
        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            templateLoader = StringTemplateLoader()
            defaultEncoding = "UTF-8"
        }
        val template = Template(templateFile.node, StringReader(content), cfg)
        StringWriter().use { writer ->
            template.process(templateFile.block(), writer)
            return writer.toString()
        }
    }
}