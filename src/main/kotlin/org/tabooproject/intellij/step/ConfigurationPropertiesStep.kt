package org.tabooproject.intellij.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.tabooproject.intellij.component.AddDeleteModuleListPanel
import org.tabooproject.intellij.createOkHttpClientWithSystemProxy
import org.tabooproject.intellij.getRequest
import org.tabooproject.intellij.util.LOCAL_MODULES
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

private fun fetchAndParseModules(
    url: String = "https://raw.githubusercontent.com/TabooLib/taboolib-gradle-plugin/master/src/main/kotlin/io/izzel/taboolib/gradle/Standards.kt",
): List<String>? {
    val client = createOkHttpClientWithSystemProxy {
        connectTimeout(5, TimeUnit.SECONDS)
        readTimeout(5, TimeUnit.SECONDS)
    }
    val request = getRequest(url)

    return try {
        val response = client.newCall(request).execute()
        response.body?.string()?.let { responseBody ->
            parseModules(responseBody)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun parseModules(content: String): List<String> {
    val pattern = """val (\w+) =""".toRegex()
    return pattern.findAll(content)
        .mapNotNull { matchResult ->
            val id = matchResult.groupValues[1]
            id.ifBlank { null }
        }
        .toList()
}

val MODULES: List<String> by lazy {
    fetchAndParseModules() ?: LOCAL_MODULES
}

val TEMPLATE_DOWNLOAD_MIRROR = mapOf(
    "github.com" to "https://github.com/TabooLib/taboolib-sdk/archive/refs/heads/idea-template.zip",
    "tabooproject.org" to "https://template.tabooproject.org"
)

data class ConfigurationProperty(
    var name: String = "untitled",
    var mainClass: String = "org.example.untitled.UntitledPlugin",
    var version: String = "1.0-SNAPSHOT",
    var mirrorIndex: String = "github.com",
    val modules: MutableList<String> = mutableListOf<String>().apply {
        add("UNIVERSAL")
        add("BUKKIT_ALL")
    },
)

class ConfigurationPropertiesStep : ModuleWizardStep() {

    private val modulePanel = AddDeleteModuleListPanel("Modules", property.modules)

    companion object {

        var property = ConfigurationProperty()
            private set

        fun refreshTemporaryData() {
            property = ConfigurationProperty()
        }
    }

    override fun getComponent(): JComponent {
        return panel {
            indent {
                group("Configuration Properties", indent = true) {
                    row("Plugin name:") {
                        textField()
                            .apply {
                                component.text = property.name
                                component.columns = 30
                            }.onChanged { property.name = it.text }
                    }
                    row("Plugin main class:") {
                        textField()
                            .apply {
                                component.text = property.mainClass
                                component.columns = 30
                            }.onChanged { property.mainClass = it.text }
                    }
                    row("Plugin version:") {
                        textField()
                            .apply {
                                component.text = property.version
                                component.columns = 30
                            }.onChanged { property.version = it.text }
                    }
                    // 不知道怎么做间隔
                    row { text("") }
                    row {
                        cell(modulePanel)
                    }
                    row { text("") }
                    row("Select template download mirror:") {
                        comboBox(TEMPLATE_DOWNLOAD_MIRROR.keys)
                            .apply {
                                component.selectedIndex = 0
                                component.columns(20)
                            }.onChanged {
                                property.mirrorIndex = it.selectedItem as String
                            }
                    }
                }
            }
        }
    }

    override fun updateDataModel() {
        // 针对控件数据 (AddDeleteListPanel) 无法直接绑定到数据模型的问题，手动导出数据
        property.modules.apply {
            clear()
            addAll(modulePanel.export())
        }
    }
}