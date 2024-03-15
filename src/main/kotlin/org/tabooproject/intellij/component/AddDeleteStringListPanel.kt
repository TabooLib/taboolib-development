package org.tabooproject.intellij.component

import com.intellij.openapi.ui.Messages
import com.intellij.ui.AddDeleteListPanel
import org.tabooproject.intellij.util.Assets

class AddDeleteStringListPanel(
    title: String,
    initial: List<String>,
    private val dialogMessage: String,
    private val dialogTitle: String
) : AddDeleteListPanel<String>(title, initial) {

    init {
        preferredSize = preferredSize.apply {
            width += 200
            height += 100
        }
    }

    override fun findItemToAdd(): String? {
        return Messages.showInputDialog(
            dialogMessage,
            dialogTitle,
            Assets.TABOO_32x32,
            "",
            null
        )
    }

    fun export() = listItems.map { it as String }
}