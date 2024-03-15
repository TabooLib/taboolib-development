package org.tabooproject.intellij

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

data class PluginConfiguration(
    var name: String,
    var mainClass: String,
    val modules: MutableMap<String, Boolean> = mutableMapOf(),
)

data class ModuleGroup(val name: String, val modules: List<String>)

val MODULE_GROUPS = listOf(
    ModuleGroup("Platform", listOf("application", "bukkit", "bungee")),
    ModuleGroup(
        "Standard",
        listOf(
            "ai",
            "chat",
            "configuration",
            "database",
            "effect",
            "kether",
            "lang",
            "metrics",
            "navigation",
            "nms",
            "nms-util",
            "porticus",
            "ui"
        )
    ),
    ModuleGroup("Expansion", listOf("alkaid-redis", "command-helper", "javascript", "persistent-container", "player-database"))
)

class ConfigurationPropertiesStep : ModuleWizardStep() {

    companion object {

        private const val DEFAULT_NAME = "untitled"
        private const val DEFAULT_MAIN_CLASS = "org.example.untitled.UntitledPlugin"

        val configuration = PluginConfiguration(
            DEFAULT_NAME,
            DEFAULT_MAIN_CLASS
        )

        fun refreshTemporaryData() {
            configuration.modules.clear()
            configuration.name = DEFAULT_NAME
            configuration.mainClass = DEFAULT_MAIN_CLASS
        }
    }

    override fun getComponent(): JComponent {
        return panel {
            indent {
                row("Plugin name:") {
                    textField()
                        .comment("The name of the plugin")
                        .bindText(configuration::name)
                }
                row("Plugin main class:") {
                    textField()
                        .comment("The main class of the plugin")
                        .bindText(configuration::mainClass)
                }
                row("Modules:") {
                    MODULE_GROUPS.forEach { group ->
                        panel {
                            row {
                                label(group.name)
                            }
                            group.modules.forEach { module ->
                                row {
                                    checkBox(module).apply {
                                        // 设置初始选中状态
                                        component.isSelected = configuration.modules.getOrDefault(module, false)
                                        // 绑定复选框的状态到映射
                                        component.addActionListener {
                                            configuration.modules[module] = component.isSelected
                                        }
                                        comment("测试信息")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun updateDataModel() = Unit
}