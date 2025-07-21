package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.tabooproject.development.step.Module
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JPanel

/**
 * 模块选择面板，提供复选框树形结构和已选模块列表显示
 * 
 * @since 1.31
 */
class CheckModulePanel(
    displayModuleList: DisplayModuleList = DisplayModuleList()
) : JBPanel<CheckModulePanel>(), Disposable {

    private val checkModuleList = CheckModuleList()
    private val displayModuleList = displayModuleList
    
    private val checkModuleScrollPane = JBScrollPane(checkModuleList)
    private val displayModuleScrollPane = JBScrollPane(displayModuleList)

    /**
     * 模块选择变更回调
     */
    var onModuleSelectionChanged: ((List<Module>) -> Unit)? = null

    init {
        // 注册子组件到自身的 disposable
        Disposer.register(this, checkModuleList as Disposable)
        Disposer.register(this, displayModuleList as Disposable)
        
        layout = BorderLayout()
        preferredSize = Dimension(600, 400)
        
        val leftPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(checkModuleScrollPane, BorderLayout.CENTER)
        }
        
        val rightPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(displayModuleScrollPane, BorderLayout.CENTER)
        }
        
        add(leftPanel, BorderLayout.WEST)
        add(rightPanel, BorderLayout.EAST)
        
        // 设置模块选择回调，连接到外部回调
        checkModuleList.onModuleSelectionChanged = { modules ->
            displayModuleList.setModules(modules)
            onModuleSelectionChanged?.invoke(modules)
        }
    }

    /**
     * 设置模块数据
     * 
     * @param modules 模块映射，key为分类名称，value为该分类下的模块列表
     */
    fun setModules(modules: Map<String, List<Module>>) {
        checkModuleList.setModules(modules)
    }

    /**
     * 获取当前选中的模块
     */
    fun getSelectedModules(): List<Module> {
        return checkModuleList.export()
    }

    /**
     * 释放资源
     */
    override fun dispose() {
        // 资源已通过Disposer.register自动释放
    }
}