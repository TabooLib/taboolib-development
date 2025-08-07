package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.tabooproject.development.step.Module
import java.awt.BorderLayout
import java.awt.Dimension

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
        preferredSize = Dimension(860, 600)
        
        // 创建更现代化的左侧面板
        val leftPanel = panel {
            group("🔍 可用模块", indent = false) {
                row {
                    text("<small>" +
                         "浏览并选择您项目的 TabooLib 模块" +
                         "</small>")
                        .apply {
                            component.border = JBUI.Borders.empty(0, 0, 8, 0)
                        }
                }
                row {
                    scrollCell(checkModuleList)
                        .apply {
                            component.preferredSize = Dimension(420, 340)
                            component.border = JBUI.Borders.compound(
                                JBUI.Borders.customLine(JBColor.border()),
                                JBUI.Borders.empty(5)
                            )
                        }
                }
            }
        }.apply {
            border = JBUI.Borders.empty(15, 15, 10, 10) // 统一边距
            background = JBColor.namedColor("Panel.background", JBColor.WHITE)
        }
        
        // 创建更优雅的右侧面板
        val rightPanel = panel {
            group("✅ 已选模块", indent = false) {
                row {
                    text("<small>" +
                         "您选择的模块 - 点击可移除" +
                         "</small>")
                        .apply {
                            component.border = JBUI.Borders.empty(0, 0, 8, 0)
                        }
                }
                row {
                    scrollCell(displayModuleList)
                        .apply {
                            component.preferredSize = Dimension(380, 340)
                            component.border = JBUI.Borders.compound(
                                JBUI.Borders.customLine(JBColor.border()),
                                JBUI.Borders.empty(3)
                            )
                            // 设置特殊的背景色表示已选状态
                            component.background = JBColor.namedColor(
                                "TextField.selectionBackground",
                                JBColor(0xf5f5f5, 0x3c3f41)
                            ).brighter()
                        }
                }
            }
        }.apply {
            border = JBUI.Borders.empty(15, 15, 10, 10) // 统一边距，与左侧保持一致
            background = JBColor.namedColor("Panel.background", JBColor.WHITE)
        }
        
        add(leftPanel, BorderLayout.CENTER)  // 左侧占据主要空间
        add(rightPanel, BorderLayout.EAST)   // 右侧固定宽度
        
        // 设置模块选择回调
        checkModuleList.onModuleSelectionChanged = { modules ->
            displayModuleList.setModules(modules)
            onModuleSelectionChanged?.invoke(modules)
        }
        
        // 设置右侧列表的点击移除回调
        displayModuleList.onModuleRemoved = { module ->
            println("DisplayModuleList: 尝试移除模块 ${module.name} (${module.id})")
            checkModuleList.unselectModule(module.id)
        }
        
        // 优化滚动条样式
        checkModuleScrollPane.apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER // 禁用水平滚动
            border = JBUI.Borders.empty()
        }
        
        displayModuleScrollPane.apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED  
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER // 禁用水平滚动
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