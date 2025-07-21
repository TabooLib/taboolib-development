package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.tabooproject.development.step.Module
import javax.swing.DefaultListModel
import javax.swing.JPanel

/**
 * 显示已选模块的列表组件
 * 
 * @since 1.31
 */
class DisplayModuleList : JPanel(), Disposable {

    private val listModel = DefaultListModel<String>()
    private val jbList = JBList(listModel)
    private val scrollPane = JBScrollPane(jbList)

    private var selectedModules: List<Module> = emptyList()

    init {
        add(scrollPane)
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
            listModel.addElement(module.name)
        }
    }

    /**
     * 添加单个模块
     * 
     * @param moduleName 模块名称
     */
    fun addModule(moduleName: String) {
        if (!listModel.contains(moduleName)) {
            listModel.addElement(moduleName)
        }
    }

    /**
     * 移除单个模块
     * 
     * @param moduleName 模块名称
     */
    fun removeModule(moduleName: String) {
        listModel.removeElement(moduleName)
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
    }
}