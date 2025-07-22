package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import org.tabooproject.development.step.Module
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.JLabel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingConstants

/**
 * 显示已选模块的列表组件
 * 
 * @since 1.31
 */
class DisplayModuleList : JPanel(), Disposable {

    private val listModel = DefaultListModel<Module>()
    private val jbList = JBList(listModel)
    private val scrollPane = JBScrollPane(jbList)

    private var selectedModules: List<Module> = emptyList()
    
    // 回调函数，当用户点击取消模块时调用
    var onModuleRemoved: ((Module) -> Unit)? = null

    init {
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
        
        // 设置自定义渲染器
        jbList.cellRenderer = ModuleListCellRenderer()
        
        // 添加鼠标监听器实现点击取消选中
        jbList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val index = jbList.locationToIndex(e.point)
                    if (index >= 0 && index < listModel.size) {
                        val module = listModel.getElementAt(index)
                        // 触发取消选中回调
                        onModuleRemoved?.invoke(module)
                    }
                }
            }
        })
        
        // 设置列表样式
        jbList.apply {
            selectionMode = javax.swing.ListSelectionModel.SINGLE_SELECTION
            layoutOrientation = JBList.VERTICAL
            visibleRowCount = -1
            fixedCellHeight = 32
            background = JBColor.WHITE
            border = JBUI.Borders.empty(4)
        }
        
        // 设置滚动面板样式
        scrollPane.apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
    }

    /**
     * 设置要显示的模块列表
     * 
     * @param modules 模块列表
     */
    fun setModules(modules: List<Module>) {
        selectedModules = modules
        listModel.clear()
        modules.forEach { module ->
            listModel.addElement(module)
        }
    }

    /**
     * 添加单个模块
     * 
     * @param module 模块对象
     */
    fun addModule(module: Module) {
        // 检查是否已存在相同ID的模块
        val exists = (0 until listModel.size).any { 
            listModel.getElementAt(it).id == module.id 
        }
        if (!exists) {
            listModel.addElement(module)
        }
    }

    /**
     * 移除单个模块
     * 
     * @param module 模块对象
     */
    fun removeModule(module: Module) {
        listModel.removeElement(module)
    }

    /**
     * 获取当前选中的模块
     * 
     * @return 选中的模块列表
     */
    fun getSelectedModules(): List<Module> {
        return selectedModules
    }

    /**
     * 清空模块列表
     */
    fun clearModules() {
        selectedModules = emptyList()
        listModel.clear()
    }

    /**
     * 释放资源
     */
    override fun dispose() {
        listModel.clear()
        selectedModules = emptyList()
        onModuleRemoved = null
    }
    
    /**
     * 自定义模块列表单元格渲染器
     */
    private inner class ModuleListCellRenderer : ListCellRenderer<Module> {
        override fun getListCellRendererComponent(
            list: javax.swing.JList<out Module>,
            value: Module,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            val label = JLabel().apply {
                text = "✕ ${value.name}"
                horizontalAlignment = SwingConstants.LEFT
                verticalAlignment = SwingConstants.CENTER
                border = JBUI.Borders.empty(6, 12, 6, 12)
                
                // 设置字体和颜色
                font = font.deriveFont(Font.PLAIN, 12f)
                
                if (isSelected) {
                    background = JBColor(0xE3F2FD, 0x2C3E50) // 浅蓝色选中背景
                    foreground = JBColor(0x1565C0, 0xECF0F1) // 深蓝色文字
                } else {
                    background = JBColor.WHITE
                    foreground = JBColor(0x37474F, 0xCFD8DC) // 深灰色文字
                }
                
                isOpaque = true
                
                // 添加hover效果提示
                toolTipText = "点击移除 ${value.name} 模块"
                
                // 设置鼠标悬浮样式
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }
            return label
        }
    }
}