package org.tabooproject.development.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.tabooproject.development.component.AddDeleteStringListPanel
import org.tabooproject.development.settings.TabooLibProjectSettings
import javax.swing.JComponent

/**
 * 可选属性配置步骤
 * 
 * @since 1.31
 */
class OptionalPropertiesStep : ModuleWizardStep(), Disposable {

    private val settings = TabooLibProjectSettings.getInstance()
    private val authorsPanel = AddDeleteStringListPanel("Authors", property.authors)
    private val dependsPanel = AddDeleteStringListPanel("Dependencies", property.depends)
    private val softDependsPanel = AddDeleteStringListPanel("Soft Dependencies", property.softDepends)

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
        val defaultAuthor = settings.getDefaultAuthor()
        if (defaultAuthor.isNotEmpty() && property.authors.isEmpty()) {
            property.authors.add(defaultAuthor)
        }
    }

    override fun getComponent(): JComponent {
        return panel {
            indent {
                group("Optional Properties", indent = true) {
                    row("Description:") {
                        textField()
                            .apply {
                                component.text = property.description
                                component.columns = 30
                            }.onChanged { property.description = it.text }
                    }
                    row("Website:") {
                        textField()
                            .apply {
                                component.text = property.website
                                component.columns = 30
                            }.onChanged { property.website = it.text }
                    }
                    row {
                        cell(authorsPanel)
                    }
                    row {
                        cell(dependsPanel)
                    }
                    row {
                        cell(softDependsPanel)
                    }
                }
            }
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
        
        // 自动更新默认作者设置
        autoUpdateDefaultAuthor()
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