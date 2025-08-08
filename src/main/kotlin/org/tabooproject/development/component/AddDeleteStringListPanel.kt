package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AddDeleteListPanel
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import javax.swing.border.CompoundBorder

/**
 * 现代化的字符串添加删除列表面板
 * 
 * @since 1.31
 */
class AddDeleteStringListPanel(
    private val panelTitle: String,
    private val initialData: MutableList<String>,
    defaultHeight: Int = 120, // 进一步减少默认高度
) : AddDeleteListPanel<String>(panelTitle, initialData.toList()), Disposable {

    init {
        preferredSize = preferredSize.apply {
            width = 380 // 减少宽度以适应滚动面板
            height = defaultHeight
        }
        
        // 设置现代化的边框样式
        border = CompoundBorder(
            JBUI.Borders.customLine(JBColor.border()),
            JBUI.Borders.empty(8)
        )
        
        // 设置背景色
        background = JBColor.namedColor("Panel.background", JBColor.WHITE)
        
        // 初始化时加载已有数据
        refreshData()
    }

    override fun findItemToAdd(): String? {
        val itemType = when (panelTitle) {
            "Authors" -> "作者名称"
            "Dependencies" -> "插件名称" 
            "Soft Dependencies" -> "插件名称"
            else -> "项目"
        }
        
        val inputValue = Messages.showInputDialog(
            this,
            "输入${itemType}:",
            "添加${panelTitle}",
            Messages.getQuestionIcon(),
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return !inputString.isNullOrBlank() && 
                           inputString.trim().length >= 2 &&
                           !myListModel.contains(inputString.trim())
                }
                
                override fun canClose(inputString: String?): Boolean {
                    return checkInput(inputString)
                }
            }
        )
        
        return inputValue?.trim()
    }

    /**
     * 刷新面板数据以反映initialData的变化
     */
    fun refreshData() {
        myListModel.clear()
        initialData.forEach { item ->
            myListModel.addElement(item)
        }
    }

    fun export(): List<String> = listItems.map { it as String }

    /**
     * 释放资源
     */
    override fun dispose() {
        // 清理资源
        myListModel.clear()
    }
}