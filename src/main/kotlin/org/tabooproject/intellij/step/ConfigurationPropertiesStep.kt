package org.tabooproject.intellij.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.panel
import org.tabooproject.intellij.component.AddDeleteModuleListPanel
import javax.swing.JComponent

enum class Module {
    AI,
    APPLICATION,
    BUKKIT,
    BUKKIT_ALL,
    BUKKIT_HOOK,
    BUKKIT_UTIL,
    BUKKIT_XSERIES,
    BUNGEE,
    CHAT,
    CONFIGURATION,
    DATABASE,
    EFFECT,
    EXPANSION_COMMAND_HELPER,
    EXPANSION_FOLIA,
    EXPANSION_GEEK_TOOL,
    EXPANSION_IOC,
    EXPANSION_JAVASCRIPT,
    EXPANSION_LANG_TOOL,
    EXPANSION_PLAYER_DATABASE,
    EXPANSION_PLAYER_FAKE_OP,
    EXPANSION_PTC,
    EXPANSION_PTC_OBJECT,
    EXPANSION_REDIS,
    EXPANSION_SUBMIT_CHAIN,
    KETHER,
    LANG,
    METRICS,
    NAVIGATION,
    NMS,
    NMS_UTIL,
    PORTICUS,
    UI,
    UNIVERSAL,
    UNIVERSAL_DATABASE,
    VELOCITY
}

data class ConfigurationProperty(
    var name: String = "untitled",
    var mainClass: String = "org.example.untitled.UntitledPlugin",
    var version: String = "1.0-SNAPSHOT",
    val modules: MutableList<Module> = mutableListOf(Module.UNIVERSAL, Module.BUKKIT_ALL),
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