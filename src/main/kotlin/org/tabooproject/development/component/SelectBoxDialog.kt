package org.tabooproject.development.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

@Deprecated("Use CheckModulePanel")
class SelectBoxDialog(inputTitle: String, private val inputMessage: String, options: Array<String>) : DialogWrapper(true) {

    private val comboBox: JComboBox<String> = ComboBox(options)

    init {
        title = inputTitle
        init()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel()
        dialogPanel.add(JLabel(inputMessage))
        dialogPanel.add(comboBox)
        return dialogPanel
    }

    fun getSelectedOption(): String? {
        return comboBox.selectedItem as? String
    }
}