package org.tabooproject.intellij.component

import com.intellij.ui.AddDeleteListPanel
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.tabooproject.intellij.step.MODULES

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
        val dialog = SelectBoxDialog("Add Module", "Choose a module:", MODULES
            .filter { it !in listItems }
            .toTypedArray()
        )
        dialog.showAndGet().ifFalse { return null }
        return MODULES.firstOrNull { it == dialog.getSelectedOption() }
    }

    fun export(): List<String> = listItems.map { it as String }
}