package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.whenSizeChanged
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
    private val displayModuleList: DisplayModuleList = DisplayModuleList()
) : JBPanel<CheckModulePanel>(), Disposable {

    private val checkModuleList = CheckModuleList()

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
        preferredSize = Dimension(800, 480) // 增加高度，充分利用空间
        
        // 创建美观的边框和标题
        val leftPanel = panel {
            group("Available Modules", indent = false) {
                row {
                    scrollCell(checkModuleList)
                        .apply {
                            component.preferredSize = Dimension(380, 410) // 增加高度
                        }
                }
            }
        }.apply {
            border = JBUI.Borders.empty(10, 10, 5, 5)
        }
        
        val rightPanel = panel {
            group("Selected Modules", indent = false) {
                row {
                    scrollCell(displayModuleList)
                        .apply {
                            component.preferredSize = Dimension(350, 410) // 增加高度与左侧对齐
                        }
                }
            }
        }.apply {
            border = JBUI.Borders.empty(10, 5, 5, 10)
        }
        
        add(leftPanel, BorderLayout.CENTER)  // 左侧占据主要空间
        add(rightPanel, BorderLayout.EAST)   // 右侧固定宽度
        
        // 设置模块选择回调，连接到外部回调
        checkModuleList.onModuleSelectionChanged = { modules ->
            displayModuleList.setModules(modules)
            onModuleSelectionChanged?.invoke(modules)
        }
        
        // 设置右侧列表的点击取消选中回调
        displayModuleList.onModuleRemoved = { module ->
            println("DisplayModuleList: 尝试移除模块 ${module.name} (${module.id})")
            // 在左侧树中取消选中该模块，不重新设置整个列表
            checkModuleList.unselectModule(module.id)
        }
        
        // 优化滚动条设置
        checkModuleScrollPane.apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            border = JBUI.Borders.empty()
        }
        
        displayModuleScrollPane.apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED  
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            border = JBUI.Borders.empty()
        }
    }

    /**
     * 设置模块数据
     * 
     * @param modules 模块映射，key为分类名称，value为该分类下的模块列表
     */
    fun setModules(modules: Map<String, List<Module>>) {
        checkModuleList.setModules(modules)

        checkModuleScrollPane.size = checkModuleScrollPane.preferredSize
    }

    /**
     * 设置选中的模块
     */
    fun setSelectedModules(moduleIds: List<String>) {
        checkModuleList.setSelectedModules(moduleIds)
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