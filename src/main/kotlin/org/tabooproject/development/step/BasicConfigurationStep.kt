package org.tabooproject.development.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.tabooproject.development.settings.TabooLibProjectSettings
import org.tabooproject.development.util.ResourceLoader
import javax.swing.JComponent

/**
 * 基础配置步骤 - 第一页
 * 包含项目基本信息和镜像设置
 *
 * @since 1.41
 */
class BasicConfigurationStep(val context: WizardContext) : ModuleWizardStep(), Disposable {

    private val settings = TabooLibProjectSettings.getInstance()
    private var modulesMirrorComboBox: com.intellij.openapi.ui.ComboBox<String>? = null

    companion object {
        var property = BasicConfigurationProperty()
            private set

        fun refreshTemporaryData() {
            property = BasicConfigurationProperty()
        }
    }

    init {
        // 设置ResourceLoader使用保存的模块镜像
        val savedModulesMirror = settings.getDefaultModulesMirror()
        if (ResourceLoader.getAvailableMirrors().containsKey(savedModulesMirror)) {
            ResourceLoader.setMirror(savedModulesMirror)
        }

        // 加载默认设置
        loadDefaultSettings()
    }

    private fun loadDefaultSettings() {
        property.projectName = context.projectName ?: ""
        property.modulesMirror = settings.getDefaultModulesMirror()
        property.templateMirror = settings.getDefaultTemplateMirror()
    }

    override fun getComponent(): JComponent {
        return panel {
            group("Project Configuration", indent = false) {
                row("Project name:") {
                    textField()
                        .apply {
                            component.text = property.projectName
                            component.columns = 35
                            component.toolTipText = "Enter your project name"
                        }.onChanged {
                            property.projectName = it.text
                            // 同步更新到 WizardContext
                            context.projectName = it.text
                        }
                }.rowComment("The name of your TabooLib project")
            }

            separator()

            group("Mirror Settings", indent = false) {
                row("Modules mirror:") {
                    comboBox(ResourceLoader.getAvailableMirrors().keys)
                        .apply {
                            modulesMirrorComboBox = component
                            component.columns(25)
                            component.toolTipText = "Choose the mirror for downloading module information"

                            // 设置当前选中的镜像
                            val savedMirror = settings.getDefaultModulesMirror()
                            val mirrorKeys = ResourceLoader.getAvailableMirrors().keys.toList()
                            val selectedIndex = mirrorKeys.indexOf(savedMirror).takeIf { it >= 0 } ?: 0
                            component.selectedIndex = selectedIndex
                        }.onChanged {
                            val mirrorKey = it.selectedItem as String
                            property.modulesMirror = mirrorKey
                            ResourceLoader.setMirror(mirrorKey)
                            // 保存到用户设置中
                            settings.setDefaultModulesMirror(mirrorKey)
                        }
                }.rowComment("Select mirror for module data download - place before module selection for better access")

                row("Template mirror:") {
                    comboBox(TEMPLATE_DOWNLOAD_MIRROR.keys)
                        .apply {
                            component.selectedIndex = 0
                            component.columns(25)
                            component.toolTipText = "Choose the mirror for downloading project template"

                            // 设置保存的模板镜像
                            val savedTemplateMirror = settings.getDefaultTemplateMirror()
                            val templateKeys = TEMPLATE_DOWNLOAD_MIRROR.keys.toList()
                            val selectedIndex = templateKeys.indexOf(savedTemplateMirror).takeIf { it >= 0 } ?: 0
                            component.selectedIndex = selectedIndex
                        }.onChanged {
                            property.templateMirror = it.selectedItem as String
                        }
                }.rowComment("Select mirror for project template download")
            }
        }
    }

    override fun updateStep() {
        property.projectName = context.projectName ?: ""
    }

    override fun updateDataModel() {
        context.projectName = property.projectName
        // 更新ConfigurationPropertiesStep的数据
        ConfigurationPropertiesStep.property.name = property.projectName
        ConfigurationPropertiesStep.property.mirrorIndex = property.templateMirror
    }

    override fun dispose() {
        // 清理资源
    }

    /**
     * 基础配置属性
     */
    data class BasicConfigurationProperty(
        var projectName: String = "",
        var modulesMirror: String = "gitee.com",
        var templateMirror: String = "tabooproject.org"
    )
}
