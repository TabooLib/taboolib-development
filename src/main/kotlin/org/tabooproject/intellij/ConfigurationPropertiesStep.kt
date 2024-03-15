package org.tabooproject.intellij

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

data class ModuleGroup(val name: String, val modules: List<String>)

val MODULE_GROUPS = listOf(
    ModuleGroup("Platform:", listOf("application", "bukkit", "bungee")),
    ModuleGroup(
        "Standard:",
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
    ModuleGroup("Expansion:", listOf("alkaid-redis", "command-helper", "javascript", "persistent-container", "player-database"))
)

class ConfigurationPropertiesStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    val pluginNameProperty = propertyGraph.property("untitled")
    val pluginMainClassPath = propertyGraph.property("org.example.untitled.UntitledPlugin")

    override fun setupUI(builder: Panel) {
        builder.group("Plugin Properties") {
            // 插件名
            row("Plugin name:") {
                textField()
                    .bindText(pluginNameProperty)
                    .columns(COLUMNS_MEDIUM)
            }
            // 主类名
            row("Main class path:") {
                textField()
                    .bindText(pluginMainClassPath)
                    .columns(COLUMNS_MEDIUM)
            }
            row("Modules:") {
                MODULE_GROUPS.forEach { group ->
                    panel {
                        row {
                            label(group.name)
                        }
                        group.modules.forEach { module ->
                            row {
                                checkBox(module).comment("这是一条测试")
                            }
                        }
                    }
                }
            }
        }
    }
}