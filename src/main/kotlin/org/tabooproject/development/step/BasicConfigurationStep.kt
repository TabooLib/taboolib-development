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
            indent {
                // 添加向导步骤指示器
                row {
                    text("<h3>第 1 步，共 3 步：项目设置</h3>" +
                         "<p>配置项目基本信息和下载设置</p>")
                        .apply {
                            component.border = com.intellij.util.ui.JBUI.Borders.empty(0, 0, 20, 0)
                        }
                }
                
                // 项目配置组
                group("📁 项目信息", indent = false) {
                    row("项目名称:") {
                        textField()
                            .focused()
                            .validationOnInput { textField ->
                                when {
                                    textField.text.isBlank() -> error("项目名称不能为空")
                                    !textField.text.matches(Regex("[a-zA-Z0-9_-]+")) -> 
                                        error("项目名称只能包含字母、数字、下划线和连字符")
                                    else -> null
                                }
                            }
                            .apply {
                                component.text = property.projectName
                                component.columns = 40
                                component.toolTipText = "输入您的项目名称\n示例：MyAwesomePlugin"
                            }.onChanged {
                                property.projectName = it.text
                                context.projectName = it.text
                            }
                    }.rowComment("<i>为您的 TabooLib 项目选择一个描述性名称</i>")
                }

                // 镜像设置组 - 使用更友好的图标和描述
                group("🌐 下载镜像", indent = false) {
                    row {
                        text("<small>" +
                             "选择距离您更近的镜像以获得更快的下载速度" +
                             "</small>")
                            .apply {
                                component.border = com.intellij.util.ui.JBUI.Borders.empty(0, 0, 10, 0)
                            }
                    }
                    
                    row("模块镜像:") {
                        comboBox(ResourceLoader.getAvailableMirrors().keys)
                            .apply {
                                modulesMirrorComboBox = component
                                component.columns(30)
                                component.toolTipText = "选择下载模块信息的镜像\n推荐：使用地理位置最近的镜像"

                                val savedMirror = settings.getDefaultModulesMirror()
                                val mirrorKeys = ResourceLoader.getAvailableMirrors().keys.toList()
                                val selectedIndex = mirrorKeys.indexOf(savedMirror).takeIf { it >= 0 } ?: 0
                                component.selectedIndex = selectedIndex
                            }.onChanged {
                                val mirrorKey = it.selectedItem as String
                                property.modulesMirror = mirrorKey
                                ResourceLoader.setMirror(mirrorKey)
                                settings.setDefaultModulesMirror(mirrorKey)
                            }
                    }.rowComment("<i>TabooLib 模块信息和描述的下载镜像</i>")

                    row("模板镜像:") {
                        comboBox(TEMPLATE_DOWNLOAD_MIRROR.keys)
                            .apply {
                                component.columns(30)
                                component.toolTipText = "选择下载项目模板的镜像\nGitHub：最新功能，可能较慢\nTabooProject：稳定版本，在中国更快"

                                val savedTemplateMirror = settings.getDefaultTemplateMirror()
                                val templateKeys = TEMPLATE_DOWNLOAD_MIRROR.keys.toList()
                                val selectedIndex = templateKeys.indexOf(savedTemplateMirror).takeIf { it >= 0 } ?: 0
                                component.selectedIndex = selectedIndex
                            }.onChanged {
                                property.templateMirror = it.selectedItem as String
                            }
                    }.rowComment("<i>项目模板和构建文件的下载镜像</i>")
                }
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
