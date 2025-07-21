package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AddDeleteListPanel

/**
 * 字符串添加删除列表面板
 * 
 * @since 1.31
 */
class AddDeleteStringListPanel(
    title: String,
    private val initialData: MutableList<String>,
    defaultHeight: Int = 175,
) : AddDeleteListPanel<String>(title, initialData.toList()), Disposable {

    init {
        preferredSize = preferredSize.apply {
            width += 300
            height += defaultHeight
        }
        
        // 初始化时加载已有数据
        refreshData()
    }

    override fun findItemToAdd(): String? {
        // 简单实现，可以扩展为输入对话框
        return null
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