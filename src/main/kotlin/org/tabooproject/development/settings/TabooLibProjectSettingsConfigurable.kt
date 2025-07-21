package org.tabooproject.development.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import org.tabooproject.development.component.AddDeleteStringListPanel
import org.tabooproject.development.step.TEMPLATE_DOWNLOAD_MIRROR
import javax.swing.JComponent
import javax.swing.JComboBox

/**
 * TabooLib 项目设置配置面板
 * 
 * 提供用户界面来配置 TabooLib 项目创建的默认设置
 * 
 * @since 1.31
 */
class TabooLibProjectSettingsConfigurable : Configurable {

    private val settings = TabooLibProjectSettings.getInstance()
    
    // UI 组件
    private lateinit var packagePrefixField: JBTextField
    private lateinit var authorField: JBTextField
    private lateinit var templateMirrorComboBox: JComboBox<String>
    private lateinit var favoriteModulesPanel: AddDeleteStringListPanel
    
    // 临时状态
    private var tempPackagePrefix: String = ""
    private var tempAuthor: String = ""
    private var tempTemplateMirror: String = ""
    private var tempFavoriteModules: MutableList<String> = mutableListOf()

    override fun getDisplayName(): String = "TabooLib Project Settings"

    override fun createComponent(): JComponent {
        // 初始化临时状态
        reset()
        
        // 创建常用模块面板
        favoriteModulesPanel = AddDeleteStringListPanel(
            "Favorite Modules",
            tempFavoriteModules,
            200
        )
        
        return panel {
            group("Default Project Settings") {
                row("Package prefix:") {
                    packagePrefixField = textField()
                        .bindText(::tempPackagePrefix)
                        .columns(30)
                        .comment("Default package prefix for new TabooLib projects")
                        .component
                }
                row("Default author:") {
                    authorField = textField()
                        .bindText(::tempAuthor)
                        .columns(30)
                        .comment("Default author name for plugin.yml")
                        .component
                }
                row("Template mirror:") {
                    templateMirrorComboBox = comboBox(TEMPLATE_DOWNLOAD_MIRROR.keys.toList())
                        .bindItem(::tempTemplateMirror.toNullableProperty())
                        .columns(20)
                        .comment("Default template download mirror")
                        .component
                }
                row {
                    cell(favoriteModulesPanel)
                        .comment("Modules that will be pre-selected in new projects")
                        .align(AlignY.TOP)
                }
                row {
                    comment("These settings will be used as defaults when creating new TabooLib projects")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return tempPackagePrefix != settings.getDefaultPackagePrefix() ||
                tempAuthor != settings.getDefaultAuthor() ||
                tempTemplateMirror != settings.getDefaultTemplateMirror() ||
                tempFavoriteModules != settings.getFavoriteModules()
    }

    override fun apply() {
        settings.setDefaultPackagePrefix(tempPackagePrefix)
        settings.setDefaultAuthor(tempAuthor)
        settings.setDefaultTemplateMirror(tempTemplateMirror)
        settings.setFavoriteModules(favoriteModulesPanel.export())
    }

    override fun reset() {
        tempPackagePrefix = settings.getDefaultPackagePrefix()
        tempAuthor = settings.getDefaultAuthor()
        tempTemplateMirror = settings.getDefaultTemplateMirror()
        tempFavoriteModules.clear()
        tempFavoriteModules.addAll(settings.getFavoriteModules())
        
        // 如果组件已创建，更新显示
        if (::favoriteModulesPanel.isInitialized) {
            // 使用export()和重新创建来更新内容
            val currentItems = favoriteModulesPanel.export().toMutableList()
            currentItems.clear()
            currentItems.addAll(tempFavoriteModules)
        }
    }

    override fun disposeUIResources() {
        if (::favoriteModulesPanel.isInitialized) {
            Disposer.dispose(favoriteModulesPanel)
        }
    }
} 