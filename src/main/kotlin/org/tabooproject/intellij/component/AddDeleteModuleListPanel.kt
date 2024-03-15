package org.tabooproject.intellij.component

import com.intellij.ui.AddDeleteListPanel
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.tabooproject.intellij.step.Module

class AddDeleteModuleListPanel(
    title: String,
    initial: List<Module>,
) : AddDeleteListPanel<Module>(title, initial) {

    init {
        preferredSize = preferredSize.apply {
            width += 250
            height += 175
        }
    }

    override fun findItemToAdd(): Module? {
        val dialog = SelectBoxDialog("Add Module", "Choose a module:", Module.values()
            .filter { it !in listItems }
            .map { it.name }
            .toTypedArray()
        )
        dialog.showAndGet().ifFalse { return null }
        return Module.valueOf(dialog.getSelectedOption()!!)
    }

    fun export() = listItems.map { it as Module }
}