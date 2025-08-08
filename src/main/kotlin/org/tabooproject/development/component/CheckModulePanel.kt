package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.tabooproject.development.step.Module
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
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

    // CheckModuleList本身就是JScrollPane，DisplayModuleList内部有JBScrollPane

    /**
     * 模块选择变更回调
     */
    var onModuleSelectionChanged: ((List<Module>) -> Unit)? = null

    init {
        // 注册子组件到自身的 disposable
        Disposer.register(this, checkModuleList as Disposable)
        Disposer.register(this, displayModuleList as Disposable)

        layout = GridBagLayout()
        
        // 创建左侧面板（75%宽度）
        val leftPanel = panel {
            group("🔍 可用模块", indent = false) {
                row {
                    text(
                        "<small>" +
                                "浏览并选择您项目的 TabooLib 模块" +
                                "</small>"
                    )
                        .apply {
                            component.border = JBUI.Borders.empty(0, 0, 8, 0)
                        }
                }
                row {
                    cell(checkModuleList)
                        .align(AlignX.FILL)
                        .apply {
                            // CheckModuleList本身就是JScrollPane，无需再包装
                            component.preferredSize = Dimension(480, 350) // 调整为75%宽度
                            component.minimumSize = Dimension(480, 100)
                            component.border = JBUI.Borders.compound(
                                JBUI.Borders.customLine(JBColor.border()),
                                JBUI.Borders.empty(5)
                            )
                        }
                }
            }
        }.apply {
            border = JBUI.Borders.empty(15, 15, 10, 5) // 右边距减少
            background = JBColor.namedColor("Panel.background", JBColor.WHITE)
        }

        // 创建右侧面板（25%宽度）
        val rightPanel = panel {
            group("✅ 已选模块", indent = false) {
                row {
                    text(
                        "<small>" +
                                "您选择的模块 - 点击可移除" +
                                "</small>"
                    )
                        .apply {
                            component.border = JBUI.Borders.empty(0, 0, 8, 0)
                        }
                }
                row {
                    cell(displayModuleList)
                        .align(AlignX.FILL)
                        .apply {
                            // DisplayModuleList内部已有滚动面板，无需再包装
                            component.preferredSize = Dimension(240, 350) // 调整为25%宽度
                            component.minimumSize = Dimension(240, 150)
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
            border = JBUI.Borders.empty(15, 5, 10, 15) // 左边距减少，右边距保持
            background = JBColor.namedColor("Panel.background", JBColor.WHITE)
        }

        // 使用GridBagLayout进行精确布局
        val leftConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.75 // 75%宽度
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.NORTHWEST // 顶部对齐
        }
        
        val rightConstraints = GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 0.25 // 25%宽度
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.NORTHWEST // 顶部对齐
        }
        
        add(leftPanel, leftConstraints)
        add(rightPanel, rightConstraints)

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
    }

    /**
     * 设置模块数据
     *
     * @param modules 模块映射，key为分类名称，value为该分类下的模块列表
     */
    fun setModules(modules: Map<String, List<Module>>) {
        checkModuleList.setModules(modules)
        // 移除多余的滚动面板大小设置，让组件自然调整
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