package org.tabooproject.development.component

import com.intellij.ui.AddDeleteListPanel

@Deprecated("Use CheckModulePanel")
class AddDeleteModuleListPanel(
    title: String,
    initial: List<String>,
    defaultHeight: Int = 175,
) : AddDeleteListPanel<String>(title, initial) {

    init {
        preferredSize = preferredSize.apply {
            width += 300
            height += defaultHeight
        }
    }

    override fun findItemToAdd(): String? {
//        val dialog = SelectBoxDialog("Add Module", "Choose a module:", MODULES
//            .filter { it !in listItems }
//            .toTypedArray()
//        )
//        dialog.showAndGet().ifFalse { return null }
//        return MODULES.firstOrNull { it == dialog.getSelectedOption() }
        return null
    }

    fun export(): List<String> = listItems.map { it as String }
}