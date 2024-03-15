package org.tabooproject.intellij.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

data class ConfigurationProperty(
    var name: String = "untitled",
    var mainClass: String = "org.example.untitled.UntitledPlugin",
    val modules: MutableMap<String, Boolean> = mutableMapOf(),
)

data class ModuleGroup(val name: String, val modules: List<String>)

val MODULE_GROUPS = listOf(
    ModuleGroup(
        "Platform",
        listOf(
            "application",
            "bukkit",
            "bungee"
        )
    ),
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
    ModuleGroup(
        "Expansion",
        listOf(
            "alkaid-redis",
            "command-helper",
            "javascript",
            "persistent-container",
            "player-database"
        )
    )
)

class ConfigurationPropertiesStep : ModuleWizardStep() {

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
                            .comment("The name of the plugin")
                            .bindText(property::name)
                    }
                    row("Plugin main class:") {
                        textField()
                            .comment("The main class of the plugin")
                            .bindText(property::mainClass)
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
                                            component.isSelected = property.modules.getOrDefault(module, false)
                                            // 绑定复选框的状态到映射
                                            component.addActionListener {
                                                property.modules[module] = component.isSelected
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
    }

    override fun updateDataModel() = Unit
}