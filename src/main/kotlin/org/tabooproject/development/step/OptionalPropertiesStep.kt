package org.tabooproject.development.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.tabooproject.development.component.AddDeleteStringListPanel
import org.tabooproject.development.settings.TabooLibProjectSettings
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

/**
 * 可选属性配置步骤
 *
 * @since 1.31
 */
class OptionalPropertiesStep : ModuleWizardStep(), Disposable {

    private val settings = TabooLibProjectSettings.getInstance()
    private val authorsPanel = AddDeleteStringListPanel("Authors", property.authors, 140)
    private val dependsPanel = AddDeleteStringListPanel("Dependencies", property.depends, 140)
    private val softDependsPanel = AddDeleteStringListPanel("Soft Dependencies", property.softDepends, 140)

    init {
        // 注册子面板到当前步骤的 disposable
        Disposer.register(this, authorsPanel)
        Disposer.register(this, dependsPanel)
        Disposer.register(this, softDependsPanel)

        // 加载默认作者设置
        loadDefaultSettings()
    }

    /**
     * 加载默认设置
     */
    private fun loadDefaultSettings() {
        // 默认设置的应用移到getComponent()中进行，确保UI组件已创建
    }

    /**
     * 更新作者面板显示
     */
    private fun updateAuthorsPanel() {
        // 刷新作者面板数据以反映property.authors的变化
        authorsPanel.refreshData()
    }

    override fun getComponent(): JComponent {
        val component = panel {
            indent {
                // 添加向导步骤指示器
                row {
                    text(
                        "<h3>第 3 步，共3 步：可选设置</h3>" +
                                "<p>为您的插件添加额外信息和依赖</p>"
                    )
                        .apply {
                            component.border = JBUI.Borders.empty(0, 0, 20, 0)
                        }
                }

                // 基本信息组
                group("📝 插件信息", indent = false) {
                    row("描述:") {
                        expandableTextField()
                            .validationOnInput { textField ->
                                when {
                                    textField.text.length > 200 -> warning("建议将描述控制在 200 字符以内")
                                    else -> null
                                }
                            }
                            .apply {
                                component.text = property.description
                                component.columns = 40
                                component.toolTipText = "插件功能的简要描述\n" +
                                        "这将显示在插件列表和文档中"
                            }.onChanged { property.description = it.text }
                    }.rowComment("<i>您的插件功能的简要描述（可选）</i>")

                    row("网站:") {
                        textField()
                            .validationOnInput { textField ->
                                when {
                                    textField.text.isNotEmpty() &&
                                            !textField.text.matches(Regex("https?://.*")) ->
                                        warning("网站地址应以 http:// 或 https:// 开头")

                                    else -> null
                                }
                            }
                            .apply {
                                component.text = property.website
                                component.columns = 40
                                component.toolTipText = "您的插件网站或仓库 URL\n" +
                                        "示例：https://github.com/username/plugin-name"
                            }.onChanged { property.website = it.text }
                    }.rowComment("<i>项目主页、GitHub 仓库或文档站点（可选）</i>")
                }

                // 开发信息组
                group("👥 开发团队", indent = false) {
                    row {
                        text("<i>插件作者和贡献者 - 您的名称将保存供未来项目使用</i>")
                            .apply {
                                component.border = JBUI.Borders.empty(0, 0, 8, 0)
                            }
                    }

                    row {
                        cell(authorsPanel)
                            .align(AlignX.FILL)
                            .apply {
                                component.border = JBUI.Borders.compound(
                                    JBUI.Borders.customLine(JBColor.border()),
                                    JBUI.Borders.empty(10)
                                )
                            }
                    }
                }

                // 依赖管理组
                group("🔗 外部依赖", indent = false) {
                    row {
                        text("<i>指定您的插件需要或可选使用的其他插件</i>")
                            .apply {
                                component.border = JBUI.Borders.empty(0, 0, 10, 0)
                            }
                    }

                    row {
                        text("<i>硬依赖 - 您的插件必须存在的插件</i>")
                            .apply {
                                component.border = JBUI.Borders.empty(0, 0, 10, 0)
                            }
                    }

                    row {
                        cell(dependsPanel)
                            .align(AlignX.FILL)
                            .apply {
                                component.border = JBUI.Borders.compound(
                                    JBUI.Borders.customLine(JBColor.border()),
                                    JBUI.Borders.empty(10)
                                )
                            }
                    }

                    row {
                        text("<i>软依赖 - 您的插件可以与之合作但不是必需的插件</i>")
                            .apply {
                                component.border = JBUI.Borders.empty(40, 0, 10, 0) // 增加上边距从0到20
                            }
                    }

                    row {
                        cell(softDependsPanel)
                            .align(AlignX.FILL)
                            .apply {
                                component.border = JBUI.Borders.compound(
                                    JBUI.Borders.customLine(JBColor.border()),
                                    JBUI.Borders.empty(10)
                                )
                            }
                    }
                }

                // 完成提示
                row {
                    text("<div>" +
                            "<b>🎉 准备创建您的 TabooLib 项目！</b><br/>" +
                            "<small>点击 '完成' 以使用配置的设置生成您的项目。</small>" +
                            "</div>")
                        .apply {
                            component.border = JBUI.Borders.empty(15, 0)
                        }
                }
            }
        }

        // 在UI组件创建后应用默认设置
        applyDefaultSettingsToUI()

        // 包装在滚动面板中
        val scrollPane = JBScrollPane(component).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = JBUI.Borders.empty()
            preferredSize = Dimension(900, 650) // 增加高度以适应更高的组件
        }

        return scrollPane
    }

    /**
     * 将默认设置应用到UI组件
     */
    private fun applyDefaultSettingsToUI() {
        val defaultAuthor = settings.getDefaultAuthor()
        if (defaultAuthor.isNotEmpty() && property.authors.isEmpty()) {
            property.authors.add(defaultAuthor)
            authorsPanel.refreshData()
        }
    }

    override fun updateDataModel() {
        // 导出数据到属性对象
        property.authors.clear()
        property.authors.addAll(authorsPanel.export())

        property.depends.clear()
        property.depends.addAll(dependsPanel.export())

        property.softDepends.clear()
        property.softDepends.addAll(softDependsPanel.export())

        // 不在这里自动更新默认作者设置，由ProjectBuilder.cleanup()统一处理
    }

    /**
     * 自动更新默认作者设置
     */
    private fun autoUpdateDefaultAuthor() {
        val currentAuthor = property.authors.firstOrNull()
        if (!currentAuthor.isNullOrEmpty()) {
            settings.setDefaultAuthor(currentAuthor)
        }
    }

    override fun dispose() {
        // 资源已通过Disposer.register自动释放
    }

    companion object {
        var property = OptionalProperty()
            private set

        fun refreshTemporaryData() {
            property = OptionalProperty()
        }
    }

    /**
     * 可选属性数据类
     */
    data class OptionalProperty(
        var description: String = "",
        var website: String = "",
        var authors: MutableList<String> = mutableListOf(),
        var depends: MutableList<String> = mutableListOf(),
        var softDepends: MutableList<String> = mutableListOf()
    )
}