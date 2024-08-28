package org.tabooproject.development.component

import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import javax.swing.JScrollPane

/**
 * @author 大阔
 * @since 2024/3/22 05:28
 */
class DisplayModuleList : JScrollPane() {

    private val listModule = CollectionListModel<String>()

    init {
        isFocusable = false
        val jbList = JBList<String>()
        jbList.model = listModule

        setViewportView(jbList)
    }

    fun addModule(module: String) {
        listModule.add(module)
        updateUI()
    }

    fun removeModule(module: String) {
        listModule.remove(module)
        updateUI()
    }


}